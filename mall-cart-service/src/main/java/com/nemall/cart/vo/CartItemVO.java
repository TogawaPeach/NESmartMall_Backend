package com.nemall.cart.vo;

import lombok.Data;

@Data
public class CartItemVO {

    /** 购物车条目 ID */
    private String cartItemId;

    /** 商品 ID */
    private String productId;

    private String name;

    private String model;

    private String coverUrl;

    /** 当前单价，单位：分；允许为 null */
    private Long price;

    /** 购物车保存的数量 */
    private Integer quantity;

    /** 当前库存；商品不存在时允许为 null */
    private Long stock;

    /**
     * AVAILABLE：正常
     * OUT_OF_STOCK：缺货
     * OFF_SHELF：下架
     * DELETED：删除或不存在
     */
    private String availability;
}