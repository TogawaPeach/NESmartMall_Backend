package com.nemall.cart.listener;

import com.nemall.cart.mapper.CartMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CartCheckoutListener {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final CartMapper cartMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    value = "mall.cart.checkout.queue",
                    durable = "true"
            ),
            exchange = @Exchange(
                    value = "mall.order.exchange",
                    type = ExchangeTypes.DIRECT,
                    durable = "true"
            ),
            key = "cart.checkout"
    ))
    @Transactional(rollbackFor = Exception.class)
    public void clearPurchasedItems(Message message) {

        JsonNode payload = JSON.readTree(message.getBody());

        Long userId = JSON.treeToValue(
                payload.required("userId"),
                Long.class
        );

        List<Long> productIds = JSON.convertValue(
                payload.required("productIds"),
                new TypeReference<List<Long>>() {}
        );

        for (Long productId : productIds) {
            // 复用现有删除 SQL，已不存在也视为成功。
            cartMapper.deleteByUserIdAndProductId(userId, productId);
        }
    }
}
