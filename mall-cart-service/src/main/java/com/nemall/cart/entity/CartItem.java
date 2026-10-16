package com.nemall.cart.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CartItem {

    /** 购物车条目 ID */
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 商品 ID */
    private Long productId;

    /** 购买数量 */
    private Integer quantity;

    /** 首次加入时间 */
    private LocalDateTime createdAt;

    /** 最后修改时间 */
    private LocalDateTime updatedAt;
}
