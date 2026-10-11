package com.nemall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品列表中一张商品卡片所需的展示数据。
 */
@Data
public class ProductCardVO {

    private Long id;

    private String name;

    private String coverUrl;

    private BigDecimal price;

    private Long sales;
}
