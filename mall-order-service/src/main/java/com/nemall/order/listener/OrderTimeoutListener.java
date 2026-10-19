package com.nemall.order.listener;

import com.nemall.order.config.OrderMqConfig;
import com.nemall.order.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OrderTimeoutListener {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final OrderTimeoutService orderTimeoutService;

    @RabbitListener(queues = OrderMqConfig.TIMEOUT_QUEUE)
    public void handleTimeout(Message message) {

        JsonNode payload = JSON.readTree(message.getBody());
        String orderNo = JSON.treeToValue(
                payload.required("orderNo"), String.class
        );

        // 经 Spring 代理调用独立服务，开启关单的 Seata 全局事务。
        orderTimeoutService.cancelExpiredOrder(orderNo);
    }
}
