package com.nemall.product.cache;

import com.nemall.product.vo.SeckillProductVO;

import java.time.Instant;
import java.util.List;

/** 整份秒杀列表的展示快照，逻辑过期时间独立于商品活动时间。 */
public record SeckillProductListCache(
        List<SeckillProductVO> data,
        Instant expireAt) {
}
