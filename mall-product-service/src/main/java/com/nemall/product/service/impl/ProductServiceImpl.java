package com.nemall.product.service.impl;

import com.nemall.product.mapper.ProductMapper;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.ProductCardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 销量排序查询的实现，校验参数并记录数据库查询耗时和失败信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    /** {@inheritDoc} */
    @Override
    public List<ProductCardVO> listProducts(String sort, Integer limit) {

        // SQL 使用固定的销量排序；其他排序值须拒绝，避免返回与请求含义不一致的数据。
        if (!"sales_desc".equals(sort)) {
            throw new IllegalArgumentException("暂时只支持按销量降序查询");
        }

        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("查询数量必须大于 0");
        }


        // 使用单调时钟计算 Mapper 调用耗时，避免系统时间调整影响测量。
        long start = System.nanoTime();
        List<ProductCardVO> products;
        try {
            products = productMapper.selectTopSales(limit);
        } catch (RuntimeException ex) {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            log.error("商品列表查询失败, sort={}, limit={}, costMs={}",
                    sort, limit, costMs, ex);
            // 保留原始异常交给上层处理，避免把数据库查询失败当作空列表返回。
            throw ex;
        }

        long costMs = (System.nanoTime() - start) / 1_000_000;
        log.info("商品列表查询完成, size={}, costMs={}", products.size(), costMs);
        log.debug("商品查询结果, products={}", products);

        return products;
    }
}
