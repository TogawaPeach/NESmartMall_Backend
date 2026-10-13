package com.nemall.product.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProductPageVO {
    // 本页商品，最多 12 件
    private  final List<ProductCardVO> list;

    // 当前筛选条件下的商品总数
    private  final long total;

    // 当前页码
    private  final int pageNum;

    // 每页固定 12 件
    private  final int pageSize = 12;
}
