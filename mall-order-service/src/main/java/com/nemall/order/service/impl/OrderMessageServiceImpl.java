package com.nemall.order.service.impl;

import com.nemall.order.config.OrderMqConfig;
import com.nemall.order.entity.OrderItem;
import com.nemall.order.service.OrderMessageService;
import com.nemall.order.vo.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.tm.api.transaction.TransactionHookAdapter;
import org.apache.seata.tm.api.transaction.TransactionHookManager;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMessageServiceImpl implements OrderMessageService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RabbitTemplate rabbitTemplate;
    private final ExecutorService orderMessageExecutor;

    @Override
    public void sendAfterGlobalCommit(
            Long userId, OrderDetailVO order, List<OrderItem> items) {
        if (!RootContext.inGlobalTransaction()) {
            throw new IllegalStateException("订单消息必须在 Seata 全局事务中注册");
        }

        String orderNo = order.getOrderNo();
        Instant payExpireAt = order.getCreatedAt().plusSeconds(15 * 60);
        Map<String, Object> cartPayload = Map.of(
                "orderNo", orderNo,
                "userId", userId,
                "productIds", items.stream().map(OrderItem::getProductId).toList()
        );
        Map<String, Object> timeoutPayload = Map.of(
                "orderNo", orderNo,
                "payExpireAt", payExpireAt.toString()
        );

        // 使用 Seata 回调，避免本地提交后、全局回滚时误发消息。
        TransactionHookManager.registerHook(new TransactionHookAdapter() {
            @Override
            public void afterCommit() {
                // 在后台发送并等待确认，不阻塞下单请求。
                orderMessageExecutor.execute(() -> send(
                        OrderMqConfig.DELAY_KEY, timeoutPayload,
                        "TIMEOUT:" + orderNo, payExpireAt
                ));
                orderMessageExecutor.execute(() -> send(
                        OrderMqConfig.CART_KEY, cartPayload,
                        "CART:" + orderNo, null
                ));
            }
        });
    }

    private void send(String routingKey, Map<String, Object> payload,
                      String messageId, Instant payExpireAt) {
        try {
            publishConfirmed(routingKey, payload, messageId, payExpireAt);
        } catch (Exception exception) {
            // 发送失败只记录日志；消费重试由监听容器统一处理。
            log.error("订单消息发送失败，routingKey={}，messageId={}",
                    routingKey, messageId, exception);
        }
    }

    private void publishConfirmed(
            String routingKey, Map<String, Object> payload,
            String messageId, Instant payExpireAt) throws Exception {

        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(messageId);

        if (payExpireAt != null) {
            // 截止时间是毫秒精度，避免将剩余时长截断后提前投递。
            long remainingMillis = payExpireAt.toEpochMilli()
                    - System.currentTimeMillis();
            if (remainingMillis > 0) {
                properties.setExpiration(Long.toString(remainingMillis));
            } else {
                routingKey = OrderMqConfig.TIMEOUT_KEY;
            }
        }

        // 发送确认标识与业务消息标识分开。
        CorrelationData correlation = new CorrelationData();
        rabbitTemplate.send(OrderMqConfig.EXCHANGE, routingKey,
                new Message(JSON.writeValueAsBytes(payload), properties),
                correlation);

        CorrelationData.Confirm confirm;
        try {
            confirm = correlation.getFuture().get(3, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }

        if (!confirm.ack() || correlation.getReturned() != null) {
            throw new IllegalStateException(
                    "订单消息被拒绝或无法路由，messageId=" + messageId
            );
        }
    }
}
