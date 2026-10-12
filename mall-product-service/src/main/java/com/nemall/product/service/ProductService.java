package com.nemall.product.service;

import com.nemall.product.vo.ProductCardVO;

import java.util.List;

/**
 * 商品列表查询业务，负责校验查询条件并提供展示数据。
 */
public interface ProductService {

    /**
     * 查询已上架且未删除的商品。
     *
     * @param sort 排序方式，目前只支持 sales_desc
     * @param limit 返回数量，范围为 1～8
     * @param categoryId 分类 ID，null 表示全部，否则范围为 1～5
     * @return 按销量降序、同销量按商品 ID 升序排列的商品列表
     */
    List<ProductCardVO> listProducts(
            String sort, Integer limit, Long categoryId);
}
