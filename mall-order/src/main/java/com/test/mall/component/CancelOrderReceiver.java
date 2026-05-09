package com.test.mall.component;

import com.test.mall.domain.MqCancelOrder;
import com.test.mall.service.OmsPortalOrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 取消订单消息的处理者
 * Created by macro on 2018/9/14.
 */
@Component
@RocketMQMessageListener(topic = "mall.order.cancel.ttl",consumerGroup = "mall-order-cancel" )
public class CancelOrderReceiver implements RocketMQListener<MqCancelOrder> {
    private static Logger LOGGER =LoggerFactory.getLogger(CancelOrderReceiver.class);
    @Autowired
    private OmsPortalOrderService portalOrderService;

//    @RabbitListener(queues = "mall.order.cancel")
//    @RabbitHandler
//    public void handle(Message message) throws IOException {
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        MqCancelOrder mqCancelOrder = objectMapper.readValue(message.getBody(), MqCancelOrder.class);
//
//
//
//        LOGGER.info("取消订单对象:{}",mqCancelOrder);
//        portalOrderService.cancelOrder(mqCancelOrder.getOrderId(),mqCancelOrder.getMemberId());
//        LOGGER.info("process orderId:{}",mqCancelOrder.getOrderId());
//    }

    @Override
    public void onMessage(MqCancelOrder mqCancelOrder) {
        LOGGER.info("取消订单对象:{}",mqCancelOrder);
        portalOrderService.cancelOrder(mqCancelOrder.getOrderId(),mqCancelOrder.getMemberId());
        LOGGER.info("process orderId:{}",mqCancelOrder.getOrderId());
    }
}
