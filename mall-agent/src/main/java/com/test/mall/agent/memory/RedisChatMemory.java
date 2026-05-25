package com.test.mall.agent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 持久化 ChatMemory - 支持跨会话记忆
 *
 * 特性：
 * 1. 对话历史持久化到 Redis，服务重启不丢失
 * 2. 自动清理过期数据（默认7天）
 * 3. 滑动窗口保留最近30条消息
 * 4. 支持用户画像关联存储
 */
@Slf4j
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final int MAX_MESSAGES = 30;
    private static final Duration EXPIRE_DAYS = Duration.ofDays(7);

    private final String memoryId;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisChatMemory(String memoryId, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.memoryId = memoryId;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(ChatMessage message) {
        String key = buildKey();
        try {
            String json = serialize(message);
            redis.opsForList().rightPush(key, json);
            redis.opsForList().trim(key, -MAX_MESSAGES, -1);
            redis.expire(key, EXPIRE_DAYS);
            log.debug("[RedisChatMemory] 添加消息: memoryId={}, type={}", memoryId, message.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("[RedisChatMemory] 添加消息失败: {}", e.getMessage());
        }
    }

    @Override
    public List<ChatMessage> messages() {
        String key = buildKey();
        List<String> jsonList = redis.opsForList().range(key, 0, -1);
        if (jsonList == null || jsonList.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChatMessage> messages = new ArrayList<>();
        for (String json : jsonList) {
            ChatMessage msg = deserialize(json);
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }

    @Override
    public void clear() {
        String key = buildKey();
        redis.delete(key);
        log.info("[RedisChatMemory] 清除记忆: memoryId={}", memoryId);
    }

    /**
     * 获取当前会话的消息数量
     */
    public long size() {
        String key = buildKey();
        Long size = redis.opsForList().size(key);
        return size != null ? size : 0;
    }

    private String buildKey() {
        return KEY_PREFIX + memoryId;
    }

    private String serialize(ChatMessage message) throws JsonProcessingException {
        MessageRecord record = new MessageRecord(
                message.getClass().getSimpleName(),
                extractText(message)
        );
        return objectMapper.writeValueAsString(record);
    }

    private ChatMessage deserialize(String json) {
        try {
            MessageRecord record = objectMapper.readValue(json, MessageRecord.class);
            return createMessage(record.type, record.text);
        } catch (Exception e) {
            log.error("[RedisChatMemory] 反序列化消息失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractText(ChatMessage message) {
        if (message instanceof UserMessage um) {
            return um.singleText();
        }
        if (message instanceof AiMessage am) {
            return am.text();
        }
        if (message instanceof SystemMessage sm) {
            return sm.text();
        }
        return "";
    }

    private ChatMessage createMessage(String type, String text) {
        return switch (type) {
            case "UserMessage" -> new UserMessage(text);
            case "AiMessage" -> new AiMessage(text);
            case "SystemMessage" -> new SystemMessage(text);
            default -> new UserMessage(text);
        };
    }

    private record MessageRecord(String type, String text) {}
}
