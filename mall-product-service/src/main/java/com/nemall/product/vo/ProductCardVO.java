package com.nemall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品列表中一张商品卡片所需的展示数据。
 */
@Data
public class ProductCardVO {

    /** 商品 ID。 */
    private Long id;

    /** 商品展示名称。 */
    private String name;

    /** 商品封面图片地址，对应数据库的 cover_image_url 字段。 */
    private String coverUrl;

    /** 商品价格，使用 BigDecimal 表示十进制金额。 */
    private BigDecimal price;

    /** 商品销量，对应数据库的 sales_count 字段，也是当前列表的主要排序依据。 */
    private Long sales;
}
