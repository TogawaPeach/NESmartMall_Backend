package com.nemall.product.service.impl;

import com.nemall.product.mapper.ProductMapper;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.ProductCardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    @Override
    public List<ProductCardVO> listProducts(String sort, Integer limit) {

        if (!"sales_desc".equals(sort)) {
            throw new IllegalArgumentException("暂时只支持按销量降序查询");
        }

        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("查询数量必须大于 0");
        }


        long start = System.nanoTime();
        List<ProductCardVO> products;
        try {
            products = productMapper.selectTopSales(limit);
        } catch (RuntimeException ex) {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            log.error("商品列表查询失败, sort={}, limit={}, costMs={}",
                    sort, limit, costMs, ex);
            throw ex;
        }

        long costMs = (System.nanoTime() - start) / 1_000_000;
        log.info("商品列表查询完成, size={}, costMs={}", products.size(), costMs);
        log.debug("商品查询结果, products={}", products);

        return products;
    }
}
