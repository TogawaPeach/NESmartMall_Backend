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
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMessageServiceImpl implements OrderMessageService {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final RetryTemplate PUBLISH_RETRY = new RetryTemplate(
            RetryPolicy.builder()
                    .maxRetries(2)
                    .delay(Duration.ofSeconds(1))
                    .multiplier(2)
                    .maxDelay(Duration.ofSeconds(2))
                    .excludes(InterruptedException.class)
                    .build()
    );

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
                // 确认和重试在后台执行，不占用下单请求等待重试间隔。
                orderMessageExecutor.execute(() -> sendTimeout(
                        timeoutPayload, "TIMEOUT:" + orderNo, payExpireAt
                ));
                orderMessageExecutor.execute(() -> sendCart(
                        cartPayload, "CART:" + orderNo
                ));
            }
        });
    }

    private void sendCart(Map<String, Object> payload, String messageId) {
        try {
            publishConfirmed(OrderMqConfig.CART_KEY, payload, messageId, null);
        } catch (Exception exception) {
            // 清购物车仍按本期约定尽力处理，不影响订单或超时消息。
            log.error("清购物车消息发送失败，messageId={}", messageId, exception);
        }
    }

    private void sendTimeout(Map<String, Object> payload,
                             String messageId, Instant payExpireAt) {
        try {
            PUBLISH_RETRY.execute(() -> {
                // 每次发送都按原截止时间重新计算剩余 TTL。
                publishConfirmed(OrderMqConfig.DELAY_KEY,
                        payload, messageId, payExpireAt);
                return null;
            });
        } catch (Exception exception) {
            log.error("超时消息发送重试耗尽，尝试保留到失败队列，messageId={}",
                    messageId, exception);

            Map<String, Object> failedPayload = new HashMap<>(payload);
            failedPayload.put("failureStage", "PUBLISH_TIMEOUT");
            failedPayload.put("failureReason", exception.toString());

            try {
                PUBLISH_RETRY.execute(() -> {
                    // 失败队列没有 TTL，不自动重新消费。
                    publishConfirmed(OrderMqConfig.FAILED_KEY,
                            failedPayload, messageId, null);
                    return null;
                });
            } catch (Exception recoveryException) {
                // Broker 持续不可用时无法依靠同一个 MQ 保存失败记录。
                // 本期不扫描、不落消息表，此处需按日志中的订单号人工处理。
                log.error("超时消息也未能进入失败队列，需按订单号人工处理，messageId={}",
                        messageId, recoveryException);
            }
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

        // 每次发送使用独立的确认标识，业务 messageId 保持不变。
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
