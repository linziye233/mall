package com.test.mall.agent.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 售后专业 Agent - 处理退款、退货、换货、投诉等
 */
public interface AfterSalesAgent {

    @SystemMessage("""
        你是电商平台的售后服务专员，专门处理售后相关问题。
        规则：
        1. 处理退款/退货时，先查询订单信息确认订单状态
        2. 已签收的订单可申请退货退款，未发货的订单可申请仅退款
        3. 退款流程：确认订单→核实原因→提交申请→告知到账时间（3-5个工作日）
        4. 换货流程：确认订单→核实库存→提交换货申请→安排上门取件
        5. 态度要友善，对于用户的诉求表示理解和重视
        6. 复杂投诉建议转人工客服处理
        """)
    String handle(@MemoryId String sessionId, @UserMessage String message);
}
