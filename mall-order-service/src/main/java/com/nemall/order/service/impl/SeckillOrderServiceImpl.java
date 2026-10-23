package com.nemall.order.service.impl;

import com.nemall.common.security.UserContext;
import com.nemall.order.config.OrderMqConfig;
import com.nemall.order.dto.CreateSeckillOrderDTO;
import com.nemall.order.exception.OrderBusinessException;
import com.nemall.order.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private static final String RESERVE_SCRIPT = loadReserveScript();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void submit(CreateSeckillOrderDTO request) throws Exception {

        Long userId = UserContext.getUserId();

        String productId = request.getProductId();
        String cacheKey =
                "product:seckill:{detail:" + productId + "}:data";

        // Lua：校验活动时间、库存 >= 1，成功后扣减一件。
        reserveStock(cacheKey);

        // 发送用户 ID、商品 ID、收货信息。
        publishOrderMessage(userId, request);
    }

    private void reserveStock(String cacheKey) {
        String now = LocalDateTime.now().toString();

        Long result = redissonClient
                .getScript(StringCodec.INSTANCE)
                .eval(
                        RScript.Mode.READ_WRITE,
                        RESERVE_SCRIPT,
                        RScript.ReturnType.LONG,
                        List.of(cacheKey),
                        now
                );

        if (result == null || result != 0L) {
            throw new OrderBusinessException(400, "下单失败");
        }
    }

    private static String loadReserveScript() {
        try {
            return new ClassPathResource("lua/seckill-reserve.lua")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("秒杀库存脚本加载失败", exception);
        }
    }

    private void publishOrderMessage(
            Long userId, CreateSeckillOrderDTO request) throws Exception {
        String messageId = UUID.randomUUID().toString();

        Map<String, Object> payload = Map.of(
                "requestId", messageId,
                "userId", userId,
                "productId", request.getProductId(),
                "quantity", 1,
                "recipient", request.getRecipient()
        );

        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(messageId);

        Message message = new Message(
                JSON.writeValueAsBytes(payload), properties
        );
        CorrelationData correlation = new CorrelationData();

        rabbitTemplate.send(
                OrderMqConfig.EXCHANGE,
                OrderMqConfig.SECKILL_KEY,
                message,
                correlation
        );

        CorrelationData.Confirm confirm =
                correlation.getFuture().get(3, TimeUnit.SECONDS);

        if (!confirm.ack() || correlation.getReturned() != null) {
            throw new OrderBusinessException(400, "下单失败");
        }
    }
}
