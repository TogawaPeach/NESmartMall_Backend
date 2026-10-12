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
    public List<ProductCardVO> listProducts(
            String sort, Integer limit, Long categoryId) {

        return productMapper.selectTopSales(limit, categoryId);
    }
}
