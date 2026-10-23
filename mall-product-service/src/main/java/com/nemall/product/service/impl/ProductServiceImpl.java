package com.nemall.product.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.api.dto.PurchaseItemsDTO;
import com.nemall.product.exception.ProductBusinessException;
import com.nemall.product.mapper.ProductMapper;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.AdminProductPageVO;
import com.nemall.product.vo.AdminProductVO;
import com.nemall.product.vo.ProductCardVO;
import com.nemall.product.vo.ProductDetailVO;
import com.nemall.product.vo.ProductPageVO;
import com.nemall.product.vo.ProductStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 销量排序查询的实现，校验参数并记录数据库查询耗时和失败信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final RedissonClient redissonClient;

    @Override
    public ProductStatsVO getProductStats() {
        return productMapper.selectProductStats();
    }

    @Override
    public AdminProductPageVO listMerchantProductsByPage(
            Integer pageNum, Integer pageSize, String status) {

        try {
            PageHelper.startPage(pageNum, pageSize, true)
                    .setReasonable(false);

            List<AdminProductVO> products =
                    productMapper.selectMerchantProducts(status);

            PageInfo<AdminProductVO> pageInfo = new PageInfo<>(products);

            return new AdminProductPageVO(
                    pageInfo.getList(),
                    pageInfo.getTotal(),
                    pageNum,
                    pageSize
            );
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(PurchaseItemsDTO request) {

        List<PurchaseItemsDTO.Item> items = request.getItems().stream()
                .sorted(Comparator.comparing(
                        PurchaseItemsDTO.Item::getProductId
                ))
                .toList();

        for (PurchaseItemsDTO.Item item : items) {
            int affectedRows = productMapper.restoreStock(
                    item.getProductId(),
                    item.getQuantity()
            );

            if (affectedRows != 1) {
                throw new ProductBusinessException(
                        409, "商品库存回补失败"
                );
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProductSummaryDTO> deductStock(
            PurchaseItemsDTO request) {

        List<PurchaseItemsDTO.Item> items = request.getItems().stream()
                .sorted(Comparator.comparing(
                        PurchaseItemsDTO.Item::getProductId
                ))
                .toList();

        for (PurchaseItemsDTO.Item item : items) {
            int affectedRows = productMapper.deductStock(
                    item.getProductId(),
                    item.getQuantity()
            );

            if (affectedRows != 1) {
                throw new ProductBusinessException(
                        409, "商品不可购买、库存不足或购买数量不合法"
                );
            }
        }

        List<Long> ids = items.stream()
                .map(PurchaseItemsDTO.Item::getProductId)
                .toList();

        return productMapper.selectByIds(ids);
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductCardVO> listProducts(
            String sort, Integer limit, Long categoryId) {

        String rankKey = categoryId == null
                ? "product:home:rank:all"
                : "product:home:rank:category:" + categoryId;

        try {
            RScoredSortedSet<String> ranking =
                    redissonClient.getScoredSortedSet(
                            rankKey, StringCodec.INSTANCE
                    );

            // 分数为商品销量，按销量从高到低读取。
            Collection<ScoredEntry<String>> entries =
                    ranking.entryRangeReversed(0, limit - 1);

            if (entries.isEmpty()) {
                return rebuildHomeRanking(rankKey, categoryId, limit);
            }

            List<Long> ids = entries.stream()
                    .map(entry -> Long.valueOf(entry.getValue()))
                    .toList();

            // 缺失的商品详情由已有方法批量查库并回填缓存。
            Map<Long, ProductDetailVO> details = loadProductDetails(ids);
            List<ProductCardVO> result = new ArrayList<>();

            for (ScoredEntry<String> entry : entries) {
                Long id = Long.valueOf(entry.getValue());
                ProductDetailVO detail = details.get(id);

                // 商品在数据库中也已不可展示时，重新查询前八补齐榜单。
                if (detail == null) {
                    return rebuildHomeRanking(rankKey, categoryId, limit);
                }

                ProductCardVO card = new ProductCardVO();
                card.setId(id);
                card.setName(detail.getName());
                card.setCoverUrl(detail.getCoverUrl());
                card.setPrice(detail.getPrice() == null
                        ? null
                        : BigDecimal.valueOf(detail.getPrice()));
                // 沿用榜单中的销量，避免详情缓存销量尚未同步。
                card.setSales(entry.getScore().longValue());
                result.add(card);
            }

            return result;

        } catch (RedisException exception) {
            log.warn("首页商品缓存操作失败，回退 SQL，categoryId={}",
                    categoryId, exception);

            return productMapper.selectTopSales(8, categoryId)
                    .stream()
                    .limit(limit)
                    .toList();
        }
    }

    private List<ProductCardVO> rebuildHomeRanking(
            String rankKey, Long categoryId, Integer limit) {

        List<ProductCardVO> products =
                productMapper.selectTopSales(8, categoryId);

        List<Long> ids = products.stream()
                .map(ProductCardVO::getId)
                .toList();

        // 复用详情缓存，冷启动时批量加载缺失的商品。
        loadProductDetails(ids);

        Map<String, Double> scores = new LinkedHashMap<>();
        for (ProductCardVO product : products) {
            scores.put(
                    product.getId().toString(),
                    product.getSales().doubleValue()
            );
        }

        Duration ttl = Duration.ofSeconds(
                600L + ThreadLocalRandom.current().nextInt(121)
        );

        // 原子替换榜单，避免首页读到只写入了一部分的数据。
        RBatch batch = redissonClient.createBatch(
                BatchOptions.defaults().executionMode(
                        BatchOptions.ExecutionMode.IN_MEMORY_ATOMIC
                )
        );

        var ranking = batch.<String>getScoredSortedSet(
                rankKey, StringCodec.INSTANCE
        );
        ranking.deleteAsync();

        if (!scores.isEmpty()) {
            ranking.addAllAsync(scores);
            ranking.expireAsync(ttl);
        }

        batch.execute();

        return products.stream()
                .limit(limit)
                .toList();
    }

    @Override
    public ProductPageVO listProductsByPage(
            Long categoryId, Integer pageNum, String keyword) {

        if (pageNum == null || pageNum < 1) {
            throw new IllegalArgumentException("pageNum 必须大于 0");
        }

        // 去掉首尾空格；未传关键词或只有空格时，不添加搜索条件
        String searchKeyword = keyword == null ? null : keyword.trim();
        if (searchKeyword != null && searchKeyword.isEmpty()) {
            searchKeyword = null;
        }

        try {
            // 当前页、每页 12 件，同时查询符合条件的总数
            PageHelper.startPage(pageNum, 12, true)
                    .setReasonable(false);

            List<ProductCardVO> products =
                    productMapper.selectProducts(categoryId, searchKeyword);

            PageInfo<ProductCardVO> pageInfo = new PageInfo<>(products);

            return new ProductPageVO(
                    pageInfo.getList(),
                    pageInfo.getTotal(),
                    pageNum
            );
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ProductSummaryDTO> queryByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return productMapper.selectByIds(ids);
    }

    @Override
    public ProductDetailVO getProductDetail(String id) {

        // 1. 获取这个商品对应的缓存操作对象。
        String key = "product:detail:" + id;
        RBucket<ProductDetailVO> bucket = redissonClient.getBucket(key);

        // 2. 读取缓存，JSON Codec 自动反序列化为商品对象。
        ProductDetailVO cached = bucket.get();
        if (cached != null) {
            return cached;
        }

        // 3. 缓存未命中，查询数据库。
        ProductDetailVO detail =
                productMapper.selectProductDetailById(id);

        if (detail == null) {
            throw new ProductBusinessException(404, "商品不存在");
        }

        // 4. 自动序列化为 JSON，并与过期时间一起写入 Redis。
        long ttlSeconds = 600L
                + ThreadLocalRandom.current().nextInt(121);

        bucket.set(detail, Duration.ofSeconds(ttlSeconds));

        return detail;
    }

    private Map<Long, ProductDetailVO> loadProductDetails(List<Long> ids) {

        if (ids.isEmpty()) {
            return Map.of();
        }

        // 1. 批量读取商品详情缓存。
        String[] keys = ids.stream()
                .map(id -> "product:detail:" + id)
                .toArray(String[]::new);

        Map<String, ProductDetailVO> cached =
                redissonClient.getBuckets().get(keys);

        Map<Long, ProductDetailVO> result = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        // 2. 收集缓存未命中的商品 ID。
        for (Long id : ids) {
            ProductDetailVO detail =
                    cached.get("product:detail:" + id);

            if (detail != null) {
                result.put(id, detail);
            } else {
                missingIds.add(id);
            }
        }

        // 3. 所有缺失商品合并为一次数据库查询。
        if (!missingIds.isEmpty()) {
            List<ProductDetailVO> details =
                    productMapper.selectProductDetailsByIds(missingIds);

            for (ProductDetailVO detail : details) {
                RBucket<ProductDetailVO> bucket =
                        redissonClient.getBucket(
                                "product:detail:" + detail.getId()
                        );

                long ttlSeconds = 600L
                        + ThreadLocalRandom.current().nextInt(121);

                bucket.set(detail, Duration.ofSeconds(ttlSeconds));

                result.put(Long.valueOf(detail.getId()), detail);
            }
        }

        return result;
    }
}
