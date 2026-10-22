package com.nemall.product;

import com.nemall.product.mapper.ProductMapper;
import com.nemall.product.service.impl.ProductServiceImpl;
import com.nemall.product.vo.ProductDetailVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RBucket;
import org.redisson.api.RBuckets;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.ScoredEntry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductDetailServiceTests {

    @Test
    void singleLookupReturnsAndCachesMappedActivityFieldsWithExistingTtl() {
        ProductMapper mapper = mock(ProductMapper.class);
        RedissonClient redis = mock(RedissonClient.class);
        RBucket<ProductDetailVO> bucket = mock();
        ProductDetailVO detail = detail();
        when(redis.<ProductDetailVO>getBucket("product:detail:25")).thenReturn(bucket);
        when(mapper.selectProductDetailById("25")).thenReturn(detail);

        ProductDetailVO result = new ProductServiceImpl(mapper, redis).getProductDetail("25");

        assertThat(result).isSameAs(detail);
        assertThat(result.getIsSeckill()).isTrue();
        assertThat(result.getSeckillStartTime()).isEqualTo("2026-10-22T16:00:00");
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(bucket).set(same(detail), ttl.capture());
        assertThat(ttl.getValue().toSeconds()).isBetween(600L, 720L);
    }

    @Test
    void homeBatchLookupCachesCompleteMappedDetailForLaterSingleLookup() {
        ProductMapper mapper = mock(ProductMapper.class);
        RedissonClient redis = mock(RedissonClient.class);
        RBucket<ProductDetailVO> bucket = mock();
        RBuckets buckets = mock(RBuckets.class);
        RScoredSortedSet<String> ranking = mock();
        ProductDetailVO detail = detail();
        when(redis.<String>getScoredSortedSet(
                "product:home:rank:all", StringCodec.INSTANCE)).thenReturn(ranking);
        when(ranking.entryRangeReversed(0, 0))
                .thenReturn(List.of(new ScoredEntry<>(7.0, "25")));
        when(redis.getBuckets()).thenReturn(buckets);
        when(buckets.<ProductDetailVO>get("product:detail:25")).thenReturn(Map.of());
        when(mapper.selectProductDetailsByIds(List.of(25L))).thenReturn(List.of(detail));
        when(redis.<ProductDetailVO>getBucket("product:detail:25")).thenReturn(bucket);

        var cards = new ProductServiceImpl(mapper, redis)
                .listProducts("sales_desc", 1, null);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().getId()).isEqualTo(25L);
        assertThat(cards.getFirst().getName()).isEqualTo("商品名称");
        ArgumentCaptor<ProductDetailVO> stored = ArgumentCaptor.forClass(ProductDetailVO.class);
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(bucket).set(stored.capture(), ttl.capture());
        assertThat(stored.getValue().getIsSeckill()).isTrue();
        assertThat(stored.getValue().getSeckillEndTime()).isEqualTo("2026-10-30T16:00:00");
        assertThat(stored.getValue().getDetailImages()).containsExactly("detail.jpg");
        assertThat(ttl.getValue().toSeconds()).isBetween(600L, 720L);
    }

    private ProductDetailVO detail() {
        return new ProductDetailVO(
                "25", "商品名称", "型号", "cover.jpg", 100L,
                7L, 10L, List.of("detail.jpg"), true,
                LocalDateTime.parse("2026-10-22T16:00:00"),
                LocalDateTime.parse("2026-10-30T16:00:00"));
    }
}
