package com.test.mall.agent.guardrails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Guardrails 安全防护服务 - 输入输出多层校验
 *
 * 安全层级：
 * 1. 输入层：Prompt Injection 检测、PII 敏感信息识别
 * 2. 执行层：工具调用权限控制（由业务层处理）
 * 3. 输出层：敏感词过滤、内容合规校验
 */
@Slf4j
@Service
public class GuardrailService {

    // Prompt Injection 攻击特征模式
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("ignore previous instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ignore all (prior|previous) (instructions|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now .*?(ignore|bypass|forget)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("DAN|jailbreak|\bmode\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system prompt|system instruction", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*?忘记.*?(规则|设定|身份)", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*?扮演.*?(黑客|攻击者|恶意).*?", Pattern.CASE_INSENSITIVE)
    );

    // PII 敏感信息模式（手机号、身份证号、银行卡号）
    private static final List<Pattern> PII_PATTERNS = Arrays.asList(
            Pattern.compile("1[3-9]\\d{9}"),                      // 手机号
            Pattern.compile("\\d{17}[\\dXx]"),                    // 身份证号
            Pattern.compile("\\d{16,19}"),                        // 银行卡号
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") // 邮箱
    );

    // 输出敏感词
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "暴力", "色情", "赌博", "毒品", "诈骗", "黑客", "攻击",
            "terrorist", "pornography", "gambling", "drugs"
    );

    /**
     * 输入层安全校验
     * @throws SecurityException 当检测到安全风险时
     */
    public void validateInput(String input) {
        if (input == null || input.isBlank()) {
            throw new SecurityException("输入内容不能为空");
        }

        if (input.length() > 2000) {
            throw new SecurityException("输入内容过长（最大2000字符）");
        }

        // 检测 Prompt Injection
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("[Guardrail] 检测到 Prompt Injection 攻击特征: {}", input.substring(0, Math.min(100, input.length())));
                throw new SecurityException("检测到潜在的安全风险输入，请重新描述您的问题");
            }
        }

        // 检测 PII（提醒而非阻断，根据业务需求可调整）
        for (Pattern pattern : PII_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.info("[Guardrail] 检测到输入中包含 PII 信息");
                // 这里可以选择：阻断 / 脱敏 / 记录
                // 当前策略：允许但记录日志
            }
        }
    }

    /**
     * 输出层安全校验 - 过滤敏感内容
     */
    public String sanitizeOutput(String output) {
        if (output == null) return "";

        String sanitized = output;

        // 敏感词替换
        for (String word : SENSITIVE_WORDS) {
            sanitized = sanitized.replaceAll(
                    "(?i)" + Pattern.quote(word),
                    "**"
            );
        }

        return sanitized;
    }

    /**
     * 输出层合规校验 - 检测幻觉风险（针对 RAG 场景）
     * @param answer LLM 生成的回答
     * @param source 知识库原文
     * @return true 表示通过校验
     */
    public boolean validateFactuality(String answer, String source) {
        if (answer == null || source == null) return true;

        // 简单启发式：如果回答中出现"不知道/不确定/可能"等词，标记为低置信度
        List<String> lowConfidenceMarkers = Arrays.asList(
                "我不知道", "不确定", "可能", "也许", "猜测", "估计",
                "I don't know", "not sure", "maybe", "possibly"
        );

        for (String marker : lowConfidenceMarkers) {
            if (answer.toLowerCase().contains(marker.toLowerCase())) {
                log.info("[Guardrail] 检测到低置信度回答，包含: {}", marker);
                return false;
            }
        }

        return true;
    }
}
