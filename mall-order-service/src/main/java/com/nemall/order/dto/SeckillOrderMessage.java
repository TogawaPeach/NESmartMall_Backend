package com.nemall.order.dto;

/** 秒杀下单消息；requestId 在消息重试时保持不变。 */
public record SeckillOrderMessage(
        String requestId,
        Long userId,
        String productId,
        Integer quantity,
        CreateSeckillOrderDTO.Recipient recipient
) {
}
