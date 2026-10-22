package com.nemall.product.service.impl;

import com.nemall.product.cache.SeckillProductListCache;
import com.nemall.product.mapper.SeckillMapper;
import com.nemall.product.service.SeckillService;
import com.nemall.product.vo.SeckillProductVO;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

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
        // 读取整份秒杀商品列表缓存。
        String cachedJson = cacheBucket().get();

        if (cachedJson == null) {
            return rebuildSeckillCache();
        }

        SeckillProductListCache cached = JSON.readValue(
                cachedJson, SeckillProductListCache.class);

        if (cached.expireAt().isAfter(Instant.now())) {
            return cached.data();
        }

        // 已逻辑过期：请求线程抢锁，将重建交给后台线程。
        RBucket<String> lock = lockBucket();
        String token = UUID.randomUUID().toString();

        if (lock.setIfAbsent(token, LOCK_TTL)) {
            cacheExecutor.execute(() -> {
                try {
                    rebuildSeckillCache(token);
                } finally {
                    lock.compareAndSet(token, null);
                }
            });
        }

        // 抢到锁和没抢到锁的请求，都不等待重建，返回旧数据。
        return cached.data();
    }

    /** 首次未命中时同步重建。 */
    private List<SeckillProductVO> rebuildSeckillCache() {
        return rebuildSeckillCache(null);
    }

    /** 同步初始化和后台刷新共用，商品 VO 直接由 MyBatis 映射。 */
    private List<SeckillProductVO> rebuildSeckillCache(String token) {
        List<SeckillProductVO> products =
                seckillMapper.selectSeckillProducts();

        SeckillProductListCache snapshot = new SeckillProductListCache(
                products, Instant.now().plus(LOGICAL_TTL));
        String json = JSON.writeValueAsString(snapshot);

        if (token == null) {
            // 首次回填时，若其他请求已建立缓存，就不覆盖。
            cacheBucket().setIfAbsent(json);
        } else {
            // 原子校验 token 后写回，锁失效的旧任务不能覆盖缓存。
            redissonClient.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    WRITE_IF_OWNER,
                    RScript.ReturnType.LONG,
                    List.of(LOCK_KEY, CACHE_KEY),
                    token, json);
        }

        return products;
    }

    private RBucket<String> cacheBucket() {
        return redissonClient.getBucket(CACHE_KEY, StringCodec.INSTANCE);
    }

    private RBucket<String> lockBucket() {
        return redissonClient.getBucket(LOCK_KEY, StringCodec.INSTANCE);
    }
}
