package com.nemall.product.controller;

import com.nemall.common.response.Result;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.ProductCardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Result<List<ProductCardVO>> listProducts(
            @RequestParam(name = "sort", defaultValue = "sales_desc") String sort,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit) {

        List<ProductCardVO> products =
                productService.listProducts(sort, limit);

        Result<List<ProductCardVO>> result = new Result<>(200, "OK", products);


        return result;
    }
}
