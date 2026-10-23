package com.nemall.product.controller;

import com.nemall.common.response.Result;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.AdminProductPageVO;
import com.nemall.product.vo.ProductStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 分页查询商家端商品列表。
     *
     * @param pageNum 页码，默认 1
     * @param pageSize 每页数量，默认 8
     * @param status 商品状态：on-sale、off-shelf；不传表示全部
     */
    @GetMapping("/page")
    public Result<AdminProductPageVO> listMerchantProductsByPage(
            @RequestParam(name = "pageNum", defaultValue = "1")
            Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "8")
            Integer pageSize,
            @RequestParam(name = "status", required = false)
            String status) {

        AdminProductPageVO pageVO =
                productService.listMerchantProductsByPage(
                        pageNum, pageSize, status
                );

        return new Result<>(200, "OK", pageVO);
    }
}
