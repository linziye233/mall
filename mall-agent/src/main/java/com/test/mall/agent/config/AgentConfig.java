package com.test.mall.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.mall.agent.agent.AfterSalesAgent;
import com.test.mall.agent.agent.OrderAgent;
import com.test.mall.agent.agent.ProductAgent;
import com.test.mall.agent.memory.RedisChatMemory;
import com.test.mall.agent.tools.OrderTools;
import com.test.mall.agent.tools.ProductTools;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Agent 组装配置 - 使用 AiServices 构建各专业 Agent
 *
 * 核心升级：使用 RedisChatMemory 替代内存记忆，支持跨会话持久化
 */
@Configuration
public class AgentConfig {

    /**
     * Redis 持久化 ChatMemory 提供者
     */
    @Bean
    public java.util.function.Function<Object, ChatMemory> chatMemoryProvider(
            StringRedisTemplate redis,
            ObjectMapper objectMapper) {
        return memoryId -> new RedisChatMemory(
                memoryId != null ? memoryId.toString() : "default",
                redis,
                objectMapper);
    }

    @Bean
    public OrderAgent orderAgent(ChatLanguageModel chatLanguageModel,
                                  OrderTools orderTools,
                                  ContentRetriever contentRetriever,
                                  java.util.function.Function<Object, ChatMemory> chatMemoryProvider) {
        return AiServices.builder(OrderAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(orderTools)
                .contentRetriever(contentRetriever)
                .build();
    }

    @Bean
    public ProductAgent productAgent(ChatLanguageModel chatLanguageModel,
                                      ProductTools productTools,
                                      ContentRetriever contentRetriever,
                                      java.util.function.Function<Object, ChatMemory> chatMemoryProvider) {
        return AiServices.builder(ProductAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(productTools)
                .contentRetriever(contentRetriever)
                .build();
    }

    @Bean
    public AfterSalesAgent afterSalesAgent(ChatLanguageModel chatLanguageModel,
                                            OrderTools orderTools,
                                            ContentRetriever contentRetriever,
                                            java.util.function.Function<Object, ChatMemory> chatMemoryProvider) {
        return AiServices.builder(AfterSalesAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(orderTools)
                .contentRetriever(contentRetriever)
                .build();
    }
}
