package com.test.mall.agent.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI 模型配置 - 由 LangChain4j Spring Boot Starter 自动注入
 * application.yml 中配置 API Key 和模型参数
 */
@Configuration
public class ModelConfig {

    /**
     * 本地 Embedding 模型（ONNX 运行时，无需外部服务）
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * 聊天模型由 langchain4j-open-ai-spring-boot-starter 自动配置
     * 基于 application.yml 中的 langchain4j.open-ai.chat-model 配置
     */
    // ChatLanguageModel 自动注入，无需手动配置

    /**
     * 流式聊天模型由 langchain4j-open-ai-spring-boot-starter 自动配置
     * 基于 application.yml 中的 langchain4j.open-ai.streaming-chat-model 配置
     */
    // StreamingChatLanguageModel 自动注入，无需手动配置
}
