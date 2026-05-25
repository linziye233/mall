package com.test.mall.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单相关工具 - 提供给 Agent 调用
 */
@Slf4j
@Component
public class OrderTools {

    private final Map<String, String> mockOrders = new HashMap<>();

    public OrderTools() {
        mockOrders.put("ORD001", "订单号：ORD001，商品：iPhone 15 Pro，金额：8999元，状态：已发货，物流：顺丰速运");
        mockOrders.put("ORD002", "订单号：ORD002，商品：MacBook Air M3，金额：10499元，状态：已签收");
        mockOrders.put("ORD003", "订单号：ORD003，商品：AirPods Pro 2，金额：1899元，状态：待发货");
    }

    @Tool("根据订单号查询订单详情，包括商品信息、金额、物流状态")
    public String queryOrder(@P("订单号，如 ORD001") String orderId) {
        log.info("[工具调用] 查询订单: {}", orderId);
        String result = mockOrders.get(orderId.toUpperCase());
        return result != null ? result : "未找到订单 " + orderId + "，请确认订单号是否正确";
    }

    @Tool("查询用户的所有订单列表")
    public String listOrders() {
        log.info("[工具调用] 查询订单列表");
        if (mockOrders.isEmpty()) {
            return "您当前没有订单记录";
        }
        StringBuilder sb = new StringBuilder("您的订单列表：\n");
        mockOrders.forEach((k, v) -> sb.append("- ").append(k).append("\n"));
        return sb.toString();
    }

    @Tool("申请订单退款，需要提供订单号和退款原因")
    public String applyRefund(@P("订单号") String orderId, @P("退款原因") String reason) {
        log.info("[工具调用] 申请退款: {}, 原因: {}", orderId, reason);
        if (!mockOrders.containsKey(orderId.toUpperCase())) {
            return "订单不存在，无法申请退款";
        }
        return "退款申请已提交，订单号：" + orderId + "，退款原因：" + reason + "，预计3-5个工作日到账";
    }

    @Tool("查询物流信息，根据订单号获取最新物流状态")
    public String queryLogistics(@P("订单号") String orderId) {
        log.info("[工具调用] 查询物流: {}", orderId);
        if (!mockOrders.containsKey(orderId.toUpperCase())) {
            return "未找到该订单的物流信息";
        }
        return "订单 " + orderId + " 物流信息：【顺丰速运】已到达北京转运中心，预计明日送达";
    }
}
