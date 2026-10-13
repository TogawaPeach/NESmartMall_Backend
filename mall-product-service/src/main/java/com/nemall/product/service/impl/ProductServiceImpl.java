package com.nemall.product.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nemall.product.mapper.ProductMapper;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.ProductCardVO;
import com.nemall.product.vo.ProductPageVO;
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

    @Override
    public ProductPageVO listProductsByPage(
            Long categoryId, Integer pageNum) {

        if (pageNum == null || pageNum < 1) {
            throw new IllegalArgumentException("pageNum 必须大于 0");
        }

        try {
            // 当前页、每页 12 件、查询商品总数
            PageHelper.startPage(pageNum, 12, true)
                    .setReasonable(false);

            // 紧接着执行需要分页的 MyBatis 查询
            List<ProductCardVO> products =
                    productMapper.selectProducts(categoryId);

            // 从查询结果中取得商品列表和总数
            PageInfo<ProductCardVO> pageInfo =
                    new PageInfo<>(products);

            return new ProductPageVO(
                    pageInfo.getList(),
                    pageInfo.getTotal(),
                    pageNum
            );
        } finally {
            PageHelper.clearPage();
        }
    }
}
