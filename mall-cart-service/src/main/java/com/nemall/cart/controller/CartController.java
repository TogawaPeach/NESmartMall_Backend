package com.nemall.cart.controller;

import com.nemall.cart.dto.AddCartItemDTO;
import com.nemall.cart.service.CartService;
import com.nemall.cart.vo.CartVO;
import com.nemall.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Result<CartVO> getCart() {
        CartVO cart = cartService.getCart();

        return new Result<>(200, "OK", cart);
    }


    @PostMapping
    public Result<Void> addToCart(
            @RequestBody AddCartItemDTO request) {

        if (request.getProductId() == null
                || request.getProductId().isBlank()
                || request.getQuantity() == null
                || request.getQuantity() <= 0) {

            throw new IllegalArgumentException(
                    "商品 ID 不能为空，加入数量必须大于 0"
            );
        }

        cartService.addToCart(request);

        return new Result<>(200, "加入购物车成功", null);
    }
}