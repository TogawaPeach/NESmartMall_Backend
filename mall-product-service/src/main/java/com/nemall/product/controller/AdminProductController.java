package com.nemall.product.controller;

import com.nemall.common.response.Result;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.ProductStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端商品管理接口。
 */
@RestController
@RequestMapping("/api/merchant/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /**
     * 查询全部商品、在售商品和下架商品的数量。
     */
    @GetMapping("/stats")
    public Result<ProductStatsVO> getProductStats() {
        ProductStatsVO stats = productService.getProductStats();
        return new Result<>(200, "OK", stats);
    }
}
