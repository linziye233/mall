package com.test.mall.agent.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 知识库服务 - 检索增强生成
 *
 * 流程：
 * 1. 用户问题向量化（Embedding）
 * 2. 在 Redis 向量库中检索最相关的知识片段
 * 3. 将检索结果作为上下文，让 LLM 生成回答
 */
@Slf4j
@Service
public class RagService {

    @Autowired
    private ContentRetriever contentRetriever;

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    /**
     * 基于知识库回答用户问题
     */
    public String answer(String question) {
        log.info("[RAG] 处理问题: {}", question);

        try {
            // 1. 检索相关知识
            Query query = Query.from(question);
            List<Content> relevantContents = contentRetriever.retrieve(query);

            if (relevantContents.isEmpty()) {
                log.info("[RAG] 未检索到相关知识，返回 null 让上层处理");
                return null;
            }

            // 2. 构建上下文
            StringBuilder context = new StringBuilder("基于以下知识库内容回答用户问题。\n\n知识库内容：\n");
            for (int i = 0; i < relevantContents.size(); i++) {
                Content content = relevantContents.get(i);
                context.append("[").append(i + 1).append("] ")
                        .append(content.textSegment().text())
                        .append("\n");
            }
            context.append("\n用户问题：").append(question).append("\n");
            context.append("要求：\n");
            context.append("- 仅基于上述知识库内容回答\n");
            context.append("- 如果知识库中没有相关信息，请明确说明\"根据现有资料无法回答\"\n");
            context.append("- 回答要简洁，控制在200字以内\n");

            // 3. 生成回答
            String answer = chatLanguageModel.generate(context.toString());
            log.info("[RAG] 生成回答: {}...", answer.substring(0, Math.min(50, answer.length())));

            return answer;

        } catch (Exception e) {
            log.error("[RAG] 处理失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检索原始知识片段（用于调试或展示引用来源）
     */
    public List<Content> retrieveSources(String question) {
        Query query = Query.from(question);
        return contentRetriever.retrieve(query);
    }
}
