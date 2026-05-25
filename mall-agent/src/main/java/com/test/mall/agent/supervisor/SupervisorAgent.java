package com.test.mall.agent.supervisor;

import com.test.mall.agent.agent.AfterSalesAgent;
import com.test.mall.agent.agent.OrderAgent;
import com.test.mall.agent.agent.ProductAgent;
import com.test.mall.agent.memory.UserProfileService;
import com.test.mall.agent.service.RagService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Supervisor Agent - 意图识别与 Agent 路由调度中心
 *
 * 核心升级：
 * 1. 集成用户画像注入（memory.UserProfileService）
 * 2. 双层意图分类（关键词 + LLM Fallback）
 * 3. 通用咨询使用 RAG 知识库回答
 */
@Slf4j
@Service
public class SupervisorAgent {

    @Autowired
    private OrderAgent orderAgent;

    @Autowired
    private ProductAgent productAgent;

    @Autowired
    private AfterSalesAgent afterSalesAgent;

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private RagService ragService;

    @Autowired
    private ContentRetriever contentRetriever;

    @Autowired
    private UserProfileService userProfileService;

    /**
     * 处理用户请求的主入口
     */
    public String handle(String message, String sessionId) {
        log.info("[Supervisor] session={}, 收到消息: {}", sessionId, message);

        // 1. 意图识别（使用原始消息，不受画像干扰）
        Intent intent = classifyIntent(message);
        log.info("[Supervisor] session={}, 意图分类: {}", sessionId, intent.getLabel());

        // 2. 注入用户画像，增强 Agent 上下文理解
        String enrichedMessage = userProfileService.enrichWithProfile(sessionId, message);
        log.debug("[Supervisor] session={}, 画像增强后的消息长度: {}", sessionId, enrichedMessage.length());

        // 3. 路由到对应 Agent
        String response = switch (intent) {
            case ORDER -> orderAgent.handle(sessionId, enrichedMessage);
            case PRODUCT -> productAgent.handle(sessionId, enrichedMessage);
            case AFTERSALES -> afterSalesAgent.handle(sessionId, enrichedMessage);
            case GENERAL -> handleGeneral(enrichedMessage, sessionId);
        };

        log.info("[Supervisor] session={}, 响应完成", sessionId);
        return response;
    }

    /**
     * 意图分类 - 两层策略：
     * 1. 关键词快速匹配（性能优先，O(1) 级响应）
     * 2. LLM 分类兜底（精度优先）
     */
    private Intent classifyIntent(String message) {
        String lower = message.toLowerCase();

        // Layer 1: 关键词快速匹配
        if (containsAny(lower, "订单", "下单", "付款", "支付", "物流", "快递", "查询订单", "我的订单")) {
            return Intent.ORDER;
        }
        if (containsAny(lower, "商品", "价格", "库存", "规格", "型号", "尺寸", "多少钱", "推荐")) {
            return Intent.PRODUCT;
        }
        if (containsAny(lower, "退款", "退货", "换货", "售后", "投诉", "质量问题", "保修")) {
            return Intent.AFTERSALES;
        }

        // Layer 2: LLM 分类兜底
        try {
            String classification = chatLanguageModel.generate(
                    "请判断以下用户咨询属于哪类意图，只返回类别编号（1/2/3/4），不要解释。\n" +
                    "1-订单相关(下单/付款/物流/查询订单)\n" +
                    "2-商品相关(价格/库存/规格/推荐)\n" +
                    "3-售后相关(退款/退货/换货/投诉)\n" +
                    "4-通用咨询(优惠活动/账户问题/其他)\n" +
                    "用户问题：\"" + message + "\""
            );

            if (classification.contains("1")) return Intent.ORDER;
            if (classification.contains("2")) return Intent.PRODUCT;
            if (classification.contains("3")) return Intent.AFTERSALES;
        } catch (Exception e) {
            log.warn("LLM 意图分类失败，降级为通用咨询: {}", e.getMessage());
        }

        return Intent.GENERAL;
    }

    /**
     * 处理通用咨询 - 使用 RAG 知识库
     */
    private String handleGeneral(String message, String sessionId) {
        try {
            // 先尝试 RAG 检索
            String ragAnswer = ragService.answer(message);
            if (ragAnswer != null && !ragAnswer.isBlank()) {
                return ragAnswer;
            }
        } catch (Exception e) {
            log.warn("RAG 检索失败: {}", e.getMessage());
        }

        // RAG 失败时使用通用回复
        return chatLanguageModel.generate(
                "你是电商平台的客服助手，请友善地回答用户问题。\n" +
                "用户问题：" + message + "\n" +
                "如果问题超出你的能力范围，请建议用户联系人工客服。"
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw.toLowerCase())) return true;
        }
        return false;
    }
}
