package com.nemall.product.controller;

import com.nemall.api.dto.ProductBatchQueryDTO;
import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.common.response.Result;
import com.nemall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @PostMapping("/batch")
    public Result<List<ProductSummaryDTO>> queryByIds(
            @RequestBody ProductBatchQueryDTO request) {

        List<ProductSummaryDTO> products =
                productService.queryByIds(request.getIds());

        return new Result<>(200, "OK", products);
    }
}