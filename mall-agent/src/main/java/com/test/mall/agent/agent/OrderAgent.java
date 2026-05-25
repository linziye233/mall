package com.test.mall.agent.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 订单专业 Agent - 处理订单查询、物流追踪、退款申请等
 */
public interface OrderAgent {

    @SystemMessage("""
        你是电商平台的订单专员，专门处理订单相关问题。
        规则：
        1. 用户查询订单时，先确认订单号，然后调用 queryOrder 工具查询
        2. 用户申请退款时，先确认订单号和退款原因，再调用 applyRefund 工具
        3. 用户查询物流时，调用 queryLogistics 工具
        4. 如果用户没有提供订单号，礼貌地请用户提供
        5. 回答要简洁明了，避免冗长
        """)
    String handle(@MemoryId String sessionId, @UserMessage String message);
}
