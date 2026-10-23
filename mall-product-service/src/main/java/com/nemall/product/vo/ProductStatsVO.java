package com.nemall.product.vo;

import lombok.Data;

/**
 * 商家端商品数量统计。
 */
@Data
public class ProductStatsVO {

    /** 全部商品数量。 */
    private Long allCount;

    /** 在售商品数量。 */
    private Long onSaleCount;

    /** 下架商品数量。 */
    private Long offShelfCount;
}
