package com.nemall.cart.dto;

import lombok.Data;

@Data
public class AddCartItemDTO {

    /** 商品 ID，前端以字符串传递 */
    private String productId;

    /** 本次加入数量 */
    private Integer quantity;
}