package com.nemall.cart.dto;

import lombok.Data;

@Data
public class UpdateCartQuantityDTO {

    /** 修改后的最终数量，必须大于 0 */
    private Integer quantity;
}
