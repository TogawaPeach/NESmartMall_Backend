package com.nemall.cart.service;

import com.nemall.cart.dto.AddCartItemDTO;
import com.nemall.cart.dto.UpdateCartQuantityDTO;
import com.nemall.cart.vo.CartVO;

public interface CartService {

    /**
     * 查询当前登录用户的购物车。
     * 用户 ID 从可信用户上下文获取。
     */
    CartVO getCart();

    /** 为当前用户加入商品 */
    void addToCart(AddCartItemDTO request);

    /** 设置当前用户某商品的最终数量，数量必须大于 0 */
    void updateQuantity(String productId, UpdateCartQuantityDTO request);

    /** 删除当前用户购物车中的指定商品 */
    void deleteCartItem(String productId);
}
