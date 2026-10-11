package com.nemall.product.service;

import com.nemall.product.vo.ProductCardVO;

import java.util.List;

/**
 * 商品列表查询业务，负责校验查询条件并提供展示数据。
 */
public interface ProductService {

    /**
     * 按销量降序查询未删除且已上架（status=1）的商品，销量相同时按商品 ID 升序排列。
     *
     * @param sort 排序方式，当前仅支持 {@code sales_desc}
     * @param limit 最多返回的商品数量，必须大于 0
     * @return 商品卡片列表；无匹配商品时返回空列表
     * @throws IllegalArgumentException 排序方式不受支持，或 limit 为 null、零或负数
     */
    List<ProductCardVO> listProducts(String sort, Integer limit);
}
