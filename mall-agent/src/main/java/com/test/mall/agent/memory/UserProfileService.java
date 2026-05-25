package com.test.mall.agent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户画像服务 - 从对话历史中提炼用户偏好
 *
 * 工作流程：
 * 1. 监听对话积累（达到阈值时触发）
 * 2. 调用 LLM 分析对话历史，提取结构化画像
 * 3. 存储到 Redis，关联用户ID
 * 4. 下次对话时注入到 Agent 上下文
 */
@Slf4j
@Service
public class UserProfileService {

    private static final String PROFILE_KEY_PREFIX = "user:profile:";
    private static final int PROFILE_EXTRACT_THRESHOLD = 5; // 达到5条消息触发提取

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 检查是否需要提取画像，并执行提取
     */
    @Async
    public void maybeExtractProfile(String userId, List<ChatMessage> messages) {
        if (messages.size() < PROFILE_EXTRACT_THRESHOLD) {
            return;
        }

        // 避免过于频繁提取（至少间隔10条消息）
        String lastExtractKey = PROFILE_KEY_PREFIX + userId + ":last_extract";
        String lastCount = redisTemplate.opsForValue().get(lastExtractKey);
        int currentCount = messages.size();
        if (lastCount != null && currentCount - Integer.parseInt(lastCount) < 10) {
            return;
        }

        try {
            UserProfile profile = extractProfile(userId, messages);
            saveProfile(userId, profile);
            redisTemplate.opsForValue().set(lastExtractKey, String.valueOf(currentCount));
            log.info("[UserProfile] 用户 {} 画像已更新", userId);
        } catch (Exception e) {
            log.error("[UserProfile] 提取画像失败: {}", e.getMessage());
        }
    }

    /**
     * 从对话历史中提取用户画像
     */
    private UserProfile extractProfile(String userId, List<ChatMessage> messages) {
        String conversation = messages.stream()
                .map(msg -> {
                    String type = msg.getClass().getSimpleName().replace("Message", "");
                    String text = extractText(msg);
                    return type + ": " + text;
                })
                .collect(Collectors.joining("\n"));

        String prompt = """
                从以下客服对话中提取用户画像。只返回纯 JSON，不要有 markdown 代码块或其他说明。

                对话记录：
                %s

                请提取以下字段（如果没有则留空数组或空字符串）：
                {
                  "preferences": ["偏好品牌或特征"],
                  "budgetRange": "预算区间描述",
                  "interestedCategories": ["感兴趣的品类"],
                  "painPoints": ["遇到的问题或不满"],
                  "recentInterests": ["最近咨询过的商品"]
                }
                """.formatted(conversation);

        String json = chatLanguageModel.generate(prompt);
        return parseProfileJson(userId, json);
    }

    /**
     * 获取用户画像（用于注入 Agent 上下文）
     */
    public UserProfile getProfile(String userId) {
        String key = PROFILE_KEY_PREFIX + userId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, UserProfile.class);
        } catch (JsonProcessingException e) {
            log.error("[UserProfile] 解析画像失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将用户画像注入到用户消息中
     */
    public String enrichWithProfile(String userId, String originalMessage) {
        UserProfile profile = getProfile(userId);
        if (profile == null) {
            return originalMessage;
        }
        return originalMessage + profile.toPromptContext();
    }

    private void saveProfile(String userId, UserProfile profile) {
        String key = PROFILE_KEY_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(profile);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            log.error("[UserProfile] 序列化画像失败: {}", e.getMessage());
        }
    }

    private UserProfile parseProfileJson(String userId, String json) {
        try {
            String clean = json.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(clean);

            return UserProfile.builder()
                    .userId(userId)
                    .preferences(parseStringArray(node.get("preferences")))
                    .budgetRange(node.has("budgetRange") ? node.get("budgetRange").asText("") : "")
                    .interestedCategories(parseStringArray(node.get("interestedCategories")))
                    .painPoints(parseStringArray(node.get("painPoints")))
                    .recentInterests(parseStringArray(node.get("recentInterests")))
                    .lastUpdated(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("[UserProfile] 解析 JSON 失败: {}", e.getMessage());
            return UserProfile.builder()
                    .userId(userId)
                    .preferences(new ArrayList<>())
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (!item.asText().isBlank()) {
                    result.add(item.asText());
                }
            }
        }
        return result;
    }

    private String extractText(ChatMessage message) {
        try {
            if (message instanceof dev.langchain4j.data.message.UserMessage um) {
                return um.singleText();
            }
            if (message instanceof dev.langchain4j.data.message.AiMessage am) {
                return am.text();
            }
        } catch (Exception e) {
            log.debug("提取消息文本失败");
        }
        return "";
    }
}
