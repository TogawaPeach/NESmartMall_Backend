package com.nemall.product.controller;

import com.nemall.common.response.Result;
import com.nemall.product.dto.SaveProductDTO;
import com.nemall.product.dto.UpdateProductStatusDTO;
import com.nemall.product.service.AdminProductService;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.AdminProductPageVO;
import com.nemall.product.vo.ProductStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private final AdminProductService adminProductService;
    private final ProductService productService;

    @PostMapping("/save")
    public Result<Void> saveProduct(@RequestBody SaveProductDTO request) {
        adminProductService.saveProduct(request);
        return new Result<>(200, "OK", null);
    }

    /**
     * 接收商品上下架请求，商家角色由 gateway 校验。
     * 商品状态更新交由 service 处理。
     */
    @PostMapping("/{id}/status")
    public Result<Void> updateProductStatus(
            @PathVariable("id") Long id,
            @RequestBody UpdateProductStatusDTO request) {

        adminProductService.updateProductStatus(id, request.getStatus());

        return new Result<>(200, "OK", null);
    }

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
