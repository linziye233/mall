package com.test.mall.agent.reflexion;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Reflexion 评估结果
 */
@Data
@Builder
public class EvaluationResult {

    /**
     * 是否通过评估
     */
    private boolean passed;

    /**
     * 置信度评分（1-10）
     */
    private int confidence;

    /**
     * 发现的问题列表
     */
    private List<String> issues;

    /**
     * 改进建议（供下一轮重试使用）
     */
    private String feedback;

    /**
     * 原始评估 JSON
     */
    private String rawEvaluation;

    public boolean isHighConfidence() {
        return confidence >= 8;
    }

    public boolean needsRetry() {
        return !passed || confidence < 6;
    }
}
