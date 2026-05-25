package com.test.mall.agent.memory;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户画像 - 从对话历史中提炼的用户偏好信息
 */
@Data
@Builder
public class UserProfile {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 偏好品牌/品类
     */
    private List<String> preferences;

    /**
     * 预算区间
     */
    private String budgetRange;

    /**
     * 感兴趣的品类
     */
    private List<String> interestedCategories;

    /**
     * 历史痛点/投诉点
     */
    private List<String> painPoints;

    /**
     * 最近关注的商品
     */
    private List<String> recentInterests;

    /**
     * 对话次数
     */
    private int conversationCount;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;

    /**
     * 格式化为 Agent Prompt 注入文本
     */
    public String toPromptContext() {
        StringBuilder sb = new StringBuilder("\n【用户画像】\n");

        if (preferences != null && !preferences.isEmpty()) {
            sb.append("- 偏好：").append(String.join("、", preferences)).append("\n");
        }
        if (budgetRange != null && !budgetRange.isBlank()) {
            sb.append("- 预算：").append(budgetRange).append("\n");
        }
        if (interestedCategories != null && !interestedCategories.isEmpty()) {
            sb.append("- 关注品类：").append(String.join("、", interestedCategories)).append("\n");
        }
        if (recentInterests != null && !recentInterests.isEmpty()) {
            sb.append("- 最近关注：").append(String.join("、", recentInterests)).append("\n");
        }
        if (painPoints != null && !painPoints.isEmpty()) {
            sb.append("- 历史痛点（需特别注意）：").append(String.join("、", painPoints)).append("\n");
        }

        return sb.toString();
    }
}
