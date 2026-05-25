package com.test.mall.agent.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static dev.langchain4j.data.document.Document.document;

/**
 * RAG 知识库配置 - Redis 向量存储
 */
@Slf4j
@Configuration
public class RagConfig {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return RedisEmbeddingStore.builder()
                .host("127.0.0.1")
                .port(6379)
                .dimension(384)
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.6)
                .build();
    }

    /**
     * 初始化知识库文档（演示数据）
     */
    @PostConstruct
    public void initKnowledgeBase() {
        log.info("初始化 RAG 知识库...");
        try {
            EmbeddingStore<TextSegment> store = embeddingStore();

            // 初始化常见问答知识
            String[] qaDocs = {
                    "退货政策：自签收之日起7天内可无理由退货，商品需保持原包装完好。特殊商品（生鲜、定制类）不支持退货。",
                    "退款到账时间：退款申请审核通过后，原路退回，信用卡3-7个工作日，支付宝/微信1-3个工作日。",
                    "配送范围：全国大部分地区支持配送，港澳台及海外地区暂不支持。偏远地区可能额外收取运费。",
                    "会员权益：注册会员享受积分累积（每消费1元积1分），积分可兑换优惠券。VIP会员额外享受95折优惠。",
                    "售后服务：商品质量问题30天内包换，180天内保修。人为损坏不在保修范围内。",
                    "优惠券使用：优惠券不可叠加使用，每个订单限用一张。优惠券过期后无法补发。",
                    "发票开具：支持电子发票和纸质发票，电子发票24小时内发送至邮箱，纸质发票随商品一起寄出。",
                    "账号安全：建议定期修改密码，开启手机验证。如发现异常登录，请立即联系客服冻结账号。"
            };

            for (String text : qaDocs) {
                Document doc = document(text);
                var segments = DocumentSplitters.recursive(200, 20)
                        .split(doc);
                for (TextSegment segment : segments) {
                    var embedding = embeddingModel.embed(segment).content();
                    store.add(embedding, segment);
                }
            }
            log.info("RAG 知识库初始化完成，共加载 {} 条文档", qaDocs.length);
        } catch (Exception e) {
            log.warn("Redis 向量库初始化失败（Redis Stack 可能未安装），RAG 功能将不可用: {}", e.getMessage());
        }
    }
}
