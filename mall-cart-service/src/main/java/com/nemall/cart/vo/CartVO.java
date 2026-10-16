package com.nemall.cart.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartVO {

    /** 当前用户的购物车条目；查询成功时不能为空引用 */
    private List<CartItemVO> items;
}