package com.nemall.product.cache;

import java.time.Instant;

/** 秒杀展示快照，逻辑过期时间独立于商品活动时间。 */
public record SeckillCache<T>(T data, Instant expireAt) {
}
