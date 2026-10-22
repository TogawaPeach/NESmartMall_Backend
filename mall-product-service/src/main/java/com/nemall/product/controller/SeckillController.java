package com.nemall.product.controller;

import com.nemall.common.response.Result;
import com.nemall.product.service.SeckillService;
import com.nemall.product.vo.ProductDetailVO;
import com.nemall.product.vo.SeckillProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seckill/products")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @GetMapping
    public Result<List<SeckillProductVO>> listSeckillProducts() {
        List<SeckillProductVO> products =
                seckillService.listSeckillProducts();

        return new Result<>(200, "OK", products);
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> getSeckillProductDetail(
            @PathVariable("id") Long id) {
        ProductDetailVO detail = seckillService.getSeckillProductDetail(id);
        return new Result<>(200, "OK", detail);
    }
}
