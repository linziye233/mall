package com.test.mall.agent.service;

import com.test.mall.agent.guardrails.GuardrailService;
import com.test.mall.agent.memory.UserProfileService;
import com.test.mall.agent.reflexion.ReflexionService;
import com.test.mall.agent.supervisor.SupervisorAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 聊天服务 - 统一入口，整合 Guardrails + Reflexion + Supervisor + 流式输出
 *
 * 核心升级：
 * 1. 集成 Reflexion 自反思（售后/投诉等高风险场景自动启用）
 * 2. 用户画像异步提取
 * 3. SSE 流式打字机效果
 */
@Slf4j
@Service
public class ChatService {

    @Autowired
    private SupervisorAgent supervisorAgent;

    @Autowired
    private GuardrailService guardrailService;

    @Autowired
    private ReflexionService reflexionService;

    /**
     * 普通聊天（非流式）
     *
     * 对售后/退款/投诉类问题自动启用 Reflexion 自反思机制，
     * 确保高准确率，避免给用户错误信息。
     */
    public String chat(String message, String sessionId) {
        // 1. 输入安全校验
        guardrailService.validateInput(message);

        // 2. 判断是否需要 Reflexion（高风险场景）
        boolean needsReflection = isHighRiskTask(message);

        String response;
        if (needsReflection) {
            log.info("[Chat] session={}, 启用 Reflexion 自反思", sessionId);
            response = reflexionService.executeWithReflection(
                    () -> supervisorAgent.handle(message, sessionId),
                    "处理用户售后问题：" + message
            );
        } else {
            response = supervisorAgent.handle(message, sessionId);
        }

        // 3. 输出安全过滤
        return guardrailService.sanitizeOutput(response);
    }

    /**
     * 流式聊天 - SSE 打字机效果
     */
    public void streamChat(String message, String sessionId, SseEmitter emitter) {
        try {
            // 1. 输入安全校验
            guardrailService.validateInput(message);

            // 2. 使用 Supervisor 获取完整回答
            String response = supervisorAgent.handle(message, sessionId);
            String sanitized = guardrailService.sanitizeOutput(response);

            // 3. 逐字发送，模拟打字机效果
            char[] chars = sanitized.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                String chunk = String.valueOf(chars[i]);
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(i))
                        .data(chunk));

                // 模拟打字延迟
                if (i < chars.length - 1) {
                    Thread.sleep(30);
                }
            }

            // 4. 发送结束标记
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();

        } catch (SecurityException e) {
            log.warn("[Chat] 安全校验不通过: {}", e.getMessage());
            try {
                emitter.send(SseEmitter.event().data("⚠️ " + e.getMessage()));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        } catch (Exception e) {
            log.error("[Chat] 流式处理异常: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 判断是否为高风险任务（需要 Reflexion）
     */
    private boolean isHighRiskTask(String message) {
        String lower = message.toLowerCase();
        return containsAny(lower, "退款", "退货", "换货", "投诉", "赔偿", "质量问题", "欺诈");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw.toLowerCase())) return true;
        }
        return false;
    }
}
