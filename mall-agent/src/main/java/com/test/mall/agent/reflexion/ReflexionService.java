package com.test.mall.agent.reflexion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Reflexion 自反思服务 - Agent 自我评估与修正
 *
 * 核心机制：
 * 1. 执行 Agent 任务获取初始结果
 * 2. Judge LLM 评估结果质量（置信度 + 问题清单）
 * 3. 如果未通过，将反思反馈注入下一轮任务
 * 4. 最多重试 3 次
 *
 * 适用场景：复杂推理任务、数据分析、文案生成等需要高准确率的场景
 */
@Slf4j
@Service
public class ReflexionService {

    @Autowired
    private ChatLanguageModel judgeModel;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;
    private static final double CONFIDENCE_THRESHOLD = 7.0;

    /**
     * 带自反思的任务执行
     *
     * @param taskExecutor 实际执行任务（如 Agent 调用）
     * @param taskDesc     任务描述（用于评估上下文）
     * @return 最终结果
     */
    public String executeWithReflection(Supplier<String> taskExecutor, String taskDesc) {
        List<String> reflectionHistory = new ArrayList<>();
        String lastResult = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            log.info("[Reflexion] 第 {} 次尝试执行: {}", attempt, taskDesc);

            // 1. 执行任务（带上历史反思）
            String enrichedTask = buildEnrichedTask(taskDesc, reflectionHistory);
            lastResult = taskExecutor.get();

            // 2. 评估结果
            EvaluationResult eval = evaluate(lastResult, enrichedTask, attempt);
            log.info("[Reflexion] 评估结果: passed={}, confidence={}, issues={}",
                    eval.isPassed(), eval.getConfidence(), eval.getIssues());

            // 3. 判断是否通过
            if (eval.isPassed() && eval.isHighConfidence()) {
                log.info("[Reflexion] 任务通过，置信度 {}", eval.getConfidence());
                return lastResult;
            }

            // 4. 记录反思，进入下一轮
            reflectionHistory.add(formatReflection(attempt, lastResult, eval));
        }

        log.warn("[Reflexion] 达到最大重试次数，返回最后一次结果");
        return lastResult;
    }

    /**
     * 执行评估
     */
    private EvaluationResult evaluate(String result, String task, int attempt) {
        String prompt = buildEvaluationPrompt(task, result, attempt);

        try {
            String evalJson = judgeModel.generate(prompt);
            return parseEvaluation(evalJson);
        } catch (Exception e) {
            log.error("[Reflexion] 评估解析失败，降级为通过: {}", e.getMessage());
            return EvaluationResult.builder()
                    .passed(true)
                    .confidence(5)
                    .issues(List.of("评估过程异常"))
                    .feedback("")
                    .rawEvaluation(e.getMessage())
                    .build();
        }
    }

    private String buildEvaluationPrompt(String task, String result, int attempt) {
        return """
            你是严格的任务质量评估专家。请评估以下任务执行结果。

            【任务】
            %s

            【执行结果】
            %s

            【要求】
            1. 结果是否正确、完整、无遗漏
            2. 是否存在事实错误或逻辑漏洞
            3. 是否满足任务的所有要求
            4. 语言是否通顺、专业

            请用 JSON 格式返回，不要其他内容：
            {
              "passed": true/false,
              "confidence": 1-10,
              "issues": ["问题1", "问题2"],
              "feedback": "具体改进建议，如果通过则填\"通过\""
            }
            """.formatted(task, result);
    }

    private EvaluationResult parseEvaluation(String evalJson) {
        try {
            // 清理可能的 markdown 代码块
            String clean = evalJson.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(clean);

            List<String> issues = new ArrayList<>();
            JsonNode issuesNode = node.get("issues");
            if (issuesNode != null && issuesNode.isArray()) {
                for (JsonNode issue : issuesNode) {
                    issues.add(issue.asText());
                }
            }

            return EvaluationResult.builder()
                    .passed(node.has("passed") && node.get("passed").asBoolean())
                    .confidence(node.has("confidence") ? node.get("confidence").asInt() : 5)
                    .issues(issues)
                    .feedback(node.has("feedback") ? node.get("feedback").asText() : "")
                    .rawEvaluation(evalJson)
                    .build();

        } catch (Exception e) {
            log.error("解析评估结果失败: {}", e.getMessage());
            return EvaluationResult.builder()
                    .passed(true)
                    .confidence(5)
                    .issues(List.of("评估解析失败"))
                    .feedback("")
                    .rawEvaluation(evalJson)
                    .build();
        }
    }

    private String buildEnrichedTask(String originalTask, List<String> reflectionHistory) {
        if (reflectionHistory.isEmpty()) {
            return originalTask;
        }
        StringBuilder sb = new StringBuilder(originalTask);
        sb.append("\n\n【历史反思，请避免重复错误】\n");
        for (String reflection : reflectionHistory) {
            sb.append(reflection).append("\n");
        }
        return sb.toString();
    }

    private String formatReflection(int attempt, String result, EvaluationResult eval) {
        return "第%d次结果：%s... | 问题：%s | 建议：%s".formatted(
                attempt,
                result.substring(0, Math.min(50, result.length())),
                String.join(", ", eval.getIssues()),
                eval.getFeedback()
        );
    }
}
