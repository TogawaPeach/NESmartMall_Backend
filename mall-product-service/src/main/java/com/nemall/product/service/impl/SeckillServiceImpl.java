package com.nemall.product.service.impl;

import com.nemall.product.cache.SeckillCache;
import com.nemall.product.exception.ProductBusinessException;
import com.nemall.product.mapper.SeckillMapper;
import com.nemall.product.service.SeckillService;
import com.nemall.product.vo.ProductDetailVO;
import com.nemall.product.vo.SeckillProductVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    private static final String CACHE_KEY = "product:seckill:{list}:data";
    private static final String LOCK_KEY = "product:seckill:{list}:lock";
    private static final Duration LOGICAL_TTL = Duration.ofSeconds(120);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    // 只有仍然持有本次重建锁的任务，才能写回缓存。
    private static final String WRITE_IF_OWNER = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                redis.call('set', KEYS[2], ARGV[2])
                return 1
            end
            return 0
            """;

    // 商品失效时清理旧快照，同样要求当前任务仍持有重建锁。
    private static final String DELETE_IF_OWNER = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[2])
            end
            return 0
            """;

    private final RedissonClient redissonClient;
    private final SeckillMapper seckillMapper;
    private final Executor cacheExecutor;

    public SeckillServiceImpl(
            RedissonClient redissonClient,
            SeckillMapper seckillMapper,
            @Qualifier("seckillCacheExecutor") Executor cacheExecutor) {
        this.redissonClient = redissonClient;
        this.seckillMapper = seckillMapper;
        this.cacheExecutor = cacheExecutor;
    }

    @Override
    public List<SeckillProductVO> listSeckillProducts() {
        return queryWithLogicalExpire(
                CACHE_KEY, LOCK_KEY,
                new TypeReference<SeckillCache<List<SeckillProductVO>>>() {},
                seckillMapper::selectSeckillProducts);
    }

    @Override
    public ProductDetailVO getSeckillProductDetail(Long id) {
        String prefix = "product:seckill:{detail:" + id + "}";
        ProductDetailVO detail = queryWithLogicalExpire(
                prefix + ":data", prefix + ":lock",
                new TypeReference<SeckillCache<ProductDetailVO>>() {},
                () -> seckillMapper.selectSeckillProductDetailById(id));

        if (detail == null) {
            throw new ProductBusinessException(404, "秒杀商品不存在");
        }
        return detail;
    }

    private <T> T queryWithLogicalExpire(
            String cacheKey, String lockKey,
            TypeReference<SeckillCache<T>> type, Supplier<T> query) {

        String cachedJson = bucket(cacheKey).get();

        if (cachedJson == null) {
            return rebuildSeckillCache(cacheKey, lockKey, null, query);
        }

        SeckillCache<T> cached = JSON.readValue(cachedJson, type);

        if (!cached.expireAt().isAfter(Instant.now())) {
            refreshSeckillCacheAsync(cacheKey, lockKey, query);
        }

        // 已过期的快照也立即返回，重建交给后台任务。
        return cached.data();
    }

    private <T> void refreshSeckillCacheAsync(
            String cacheKey, String lockKey, Supplier<T> query) {
        RBucket<String> lock = bucket(lockKey);
        String token = UUID.randomUUID().toString();

        if (!lock.setIfAbsent(token, LOCK_TTL)) {
            return;
        }

        try {
            cacheExecutor.execute(() -> {
                try {
                    rebuildSeckillCache(cacheKey, lockKey, token, query);
                } catch (RuntimeException exception) {
                    log.warn("秒杀缓存刷新失败，key={}", cacheKey, exception);
                } finally {
                    lock.compareAndSet(token, null);
                }
            });
        } catch (RejectedExecutionException exception) {
            lock.compareAndSet(token, null);
            log.warn("秒杀缓存刷新任务被拒绝，key={}", cacheKey, exception);
        }
    }

    /** 列表和详情、同步初始化和后台刷新共用同一重建流程。 */
    private <T> T rebuildSeckillCache(
            String cacheKey, String lockKey, String token, Supplier<T> query) {
        T data = query.get();

        // 空结果不写缓存；后台确认商品失效时，按锁归属清理旧快照。
        if (data == null) {
            if (token != null) {
                redissonClient.getScript(StringCodec.INSTANCE).eval(
                        RScript.Mode.READ_WRITE,
                        DELETE_IF_OWNER,
                        RScript.ReturnType.LONG,
                        List.of(lockKey, cacheKey),
                        token);
            }
            return null;
        }

        SeckillCache<T> snapshot = new SeckillCache<>(
                data, Instant.now().plus(LOGICAL_TTL));
        String json = JSON.writeValueAsString(snapshot);

        if (token == null) {
            // 首次回填时，若其他请求已建立缓存，就不覆盖。
            bucket(cacheKey).setIfAbsent(json);
        } else {
            // 原子校验 token 后写回，锁失效的旧任务不能覆盖缓存。
            redissonClient.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    WRITE_IF_OWNER,
                    RScript.ReturnType.LONG,
                    List.of(lockKey, cacheKey),
                    token, json);
        }

        return data;
    }

    private RBucket<String> bucket(String key) {
        return redissonClient.getBucket(key, StringCodec.INSTANCE);
    }
}
