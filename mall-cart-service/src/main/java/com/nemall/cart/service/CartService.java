package com.nemall.cart.service;

import com.nemall.cart.vo.CartVO;

public interface CartService {

    /**
     * 查询当前登录用户的购物车。
     * 用户 ID 从可信用户上下文获取。
     */
    CartVO getCart();
}