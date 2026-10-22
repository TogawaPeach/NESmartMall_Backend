package com.nemall.product;

import com.nemall.product.exception.ProductBusinessException;
import com.nemall.product.mapper.SeckillMapper;
import com.nemall.product.service.impl.SeckillServiceImpl;
import com.nemall.product.vo.ProductDetailVO;
import com.nemall.product.vo.SeckillProductVO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Run against a dedicated Redis with -Dtest.redis.address=redis://127.0.0.1:PORT. */
@EnabledIfSystemProperty(named = "test.redis.address", matches = ".+")
class SeckillCacheIntegrationTests {

    private static final String DETAIL_KEY = "product:seckill:{detail:25}:data";
    private static final String DETAIL_LOCK = "product:seckill:{detail:25}:lock";
    private static final String LIST_KEY = "product:seckill:{list}:data";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static RedissonClient redis;

    private final StubMapper mapper = new StubMapper();
    private final List<Runnable> pending = new ArrayList<>();
    private SeckillServiceImpl service;

    @BeforeAll
    static void connect() {
        Config config = new Config();
        config.setNettyThreads(2).setThreads(2);
        config.useSingleServer()
                .setAddress(System.getProperty("test.redis.address"))
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(2);
        redis = Redisson.create(config);
    }

    @AfterAll
    static void disconnect() {
        if (redis != null) {
            redis.shutdown();
        }
    }

    @BeforeEach
    void reset() {
        redis.getKeys().deleteByPattern("product:seckill:*");
        service = new SeckillServiceImpl(redis, mapper, pending::add);
    }

    @Test
    void cachesCompleteDetailAndReadsItWithoutAnotherDatabaseQuery() {
        ProductDetailVO first = service.getSeckillProductDetail(25L);
        mapper.detail = null;

        ProductDetailVO cached = service.getSeckillProductDetail(25L);

        assertThat(cached).isEqualTo(first);
        assertThat(cached.getDetailImages()).containsExactly("detail.jpg");
        assertThat(cached.getIsSeckill()).isTrue();
        assertThat(cached.getSeckillStartTime()).isEqualTo("2026-10-22T16:00:00");
        assertThat(cached.getSeckillEndTime()).isEqualTo("2026-10-30T16:00:00");
        assertThat(mapper.detailReads).isEqualTo(1);
        assertThat(bucket(DETAIL_KEY).remainTimeToLive()).isEqualTo(-1);
    }

    @Test
    void returnsStaleDetailBeforeRunningRefreshAndThenExposesNewStock() {
        seed(DETAIL_KEY, detail(10L), Instant.EPOCH);
        mapper.detail = detail(3L);

        assertThat(service.getSeckillProductDetail(25L).getStock()).isEqualTo(10L);
        assertThat(mapper.detailReads).isZero();
        assertThat(pending).hasSize(1);

        pending.removeFirst().run();

        assertThat(service.getSeckillProductDetail(25L).getStock()).isEqualTo(3L);
        assertThat(bucket(DETAIL_LOCK).isExists()).isFalse();
    }

    @Test
    void concurrentStaleRequestsScheduleOnlyOneRefresh() {
        seed(DETAIL_KEY, detail(10L), Instant.EPOCH);

        service.getSeckillProductDetail(25L);
        service.getSeckillProductDetail(25L);

        assertThat(pending).hasSize(1);
        assertThat(mapper.detailReads).isZero();
    }

    @Test
    void listAndDetailUseIndependentLocksAndPreserveTypedListElements() {
        seed(LIST_KEY, mapper.products, Instant.EPOCH);
        seed(DETAIL_KEY, detail(10L), Instant.EPOCH);

        assertThat(service.listSeckillProducts().getFirst().getName())
                .isEqualTo("秒杀商品");
        service.getSeckillProductDetail(25L);
        assertThat(pending).hasSize(2);

        pending.forEach(Runnable::run);

        assertThat(service.listSeckillProducts().getFirst().getId()).isEqualTo("25");
        assertThat(mapper.listReads).isEqualTo(1);
        assertThat(mapper.detailReads).isEqualTo(1);
    }

    @Test
    void missingDetailIsNeverCached() {
        mapper.detail = null;

        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> service.getSeckillProductDetail(25L))
                    .isInstanceOfSatisfying(ProductBusinessException.class,
                            exception -> assertThat(exception.getCode()).isEqualTo(404));
        }

        assertThat(bucket(DETAIL_KEY).isExists()).isFalse();
        assertThat(mapper.detailReads).isEqualTo(2);
    }

    @Test
    void invalidIdIsPassedToQueryWithoutServiceValidation() {
        mapper.detail = null;

        assertThatThrownBy(() -> service.getSeckillProductDetail(-1L))
                .isInstanceOfSatisfying(ProductBusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(404));

        assertThat(mapper.lastId).isEqualTo(-1L);
    }

    @Test
    void refreshRemovesSnapshotWhenProductNoLongerExists() {
        seed(DETAIL_KEY, detail(10L), Instant.EPOCH);
        mapper.detail = null;

        assertThat(service.getSeckillProductDetail(25L)).isNotNull();
        pending.removeFirst().run();

        assertThat(bucket(DETAIL_KEY).isExists()).isFalse();
        assertThat(bucket(DETAIL_LOCK).isExists()).isFalse();
    }

    @Test
    void refreshCannotOverwriteOrUnlockANewerOwner() {
        seed(DETAIL_KEY, detail(10L), Instant.EPOCH);
        service.getSeckillProductDetail(25L);
        bucket(DETAIL_LOCK).set("new-owner");
        seed(DETAIL_KEY, detail(2L), Instant.now().plusSeconds(120));

        pending.removeFirst().run();

        assertThat(service.getSeckillProductDetail(25L).getStock()).isEqualTo(2L);
        assertThat(bucket(DETAIL_LOCK).get()).isEqualTo("new-owner");
    }

    @Test
    void missingResultCannotDeleteANewerOwnersSnapshot() {
        seed(DETAIL_KEY, detail(10L), Instant.EPOCH);
        service.getSeckillProductDetail(25L);
        mapper.detail = null;
        bucket(DETAIL_LOCK).set("new-owner");
        seed(DETAIL_KEY, detail(2L), Instant.now().plusSeconds(120));

        pending.removeFirst().run();

        assertThat(service.getSeckillProductDetail(25L).getStock()).isEqualTo(2L);
        assertThat(bucket(DETAIL_LOCK).get()).isEqualTo("new-owner");
    }

    @Test
    void rejectedRefreshKeepsStaleDataAndReleasesLock() {
        seed(DETAIL_KEY, detail(10L), Instant.EPOCH);
        Executor rejecting = task -> { throw new RejectedExecutionException("busy"); };
        service = new SeckillServiceImpl(redis, mapper, rejecting);

        assertThat(service.getSeckillProductDetail(25L).getStock()).isEqualTo(10L);
        assertThat(bucket(DETAIL_LOCK).isExists()).isFalse();
        assertThat(mapper.detailReads).isZero();
    }

    @Test
    void failedRefreshKeepsStaleDataAndReleasesLock() {
        seed(DETAIL_KEY, detail(10L), Instant.EPOCH);
        mapper.failure = new IllegalStateException("database unavailable");
        service.getSeckillProductDetail(25L);

        pending.removeFirst().run();

        assertThat(bucket(DETAIL_LOCK).isExists()).isFalse();
        assertThat(service.getSeckillProductDetail(25L).getStock()).isEqualTo(10L);
    }

    private static RBucket<String> bucket(String key) {
        return redis.getBucket(key, StringCodec.INSTANCE);
    }

    private static void seed(String key, Object data, Instant expireAt) {
        bucket(key).set(JSON.writeValueAsString(Map.of("data", data, "expireAt", expireAt)));
    }

    private static ProductDetailVO detail(Long stock) {
        ProductDetailVO detail = new ProductDetailVO(
                "25", "秒杀商品", "型号", "cover.jpg", 100L,
                0L, stock, List.of("detail.jpg"));
        detail.setIsSeckill(true);
        detail.setSeckillStartTime(LocalDateTime.parse("2026-10-22T16:00:00"));
        detail.setSeckillEndTime(LocalDateTime.parse("2026-10-30T16:00:00"));
        return detail;
    }

    private static class StubMapper implements SeckillMapper {
        private ProductDetailVO detail = SeckillCacheIntegrationTests.detail(10L);
        private final List<SeckillProductVO> products = List.of(new SeckillProductVO(
                "25", "秒杀商品", "型号", "cover.jpg", 100L, null, null, true));
        private int detailReads;
        private int listReads;
        private Long lastId;
        private RuntimeException failure;

        @Override
        public List<SeckillProductVO> selectSeckillProducts() {
            listReads++;
            return products;
        }

        @Override
        public ProductDetailVO selectSeckillProductDetailById(Long id) {
            detailReads++;
            lastId = id;
            if (failure != null) {
                throw failure;
            }
            return detail;
        }
    }
}
