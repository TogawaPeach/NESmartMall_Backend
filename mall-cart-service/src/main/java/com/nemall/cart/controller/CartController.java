package com.nemall.cart.controller;

import com.nemall.cart.service.CartService;
import com.nemall.cart.vo.CartVO;
import com.nemall.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}