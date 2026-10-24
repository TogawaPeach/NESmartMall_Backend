package com.nemall.product.cache;

import com.nemall.product.exception.ProductBusinessException;
import com.nemall.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** 商品 ID 布隆过滤器，启动时全量预热，新增商品时增量添加。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductBloomFilter implements ApplicationRunner {

    private static final String BLOOM_KEY = "product:bloom:v1";

    private final RedissonClient redissonClient;
    private final ProductMapper productMapper;

    @Override
    public void run(ApplicationArguments args) {
        warmUp();
    }

    public void warmUp() {
        RBloomFilter<Long> bloom = redissonClient.getBloomFilter(BLOOM_KEY);
        bloom.tryInit(1_000_000L, 0.001);

        // 包含下架商品，重新上架时无需再次添加 ID；不清空已有过滤器。
        List<Long> ids = productMapper.selectAllProductIds();
        ids.forEach(bloom::add);
        log.info("商品布隆过滤器预热完成，加载 {} 个商品 ID", ids.size());
    }

    public void check(String id) {
        RBloomFilter<Long> bloom = redissonClient.getBloomFilter(BLOOM_KEY);
        if (!bloom.contains(Long.valueOf(id))) {
            throw new ProductBusinessException(404, "商品不存在");
        }
    }

    public void add(Long id) {
        RBloomFilter<Long> bloom = redissonClient.getBloomFilter(BLOOM_KEY);
        bloom.add(id);
    }
}
