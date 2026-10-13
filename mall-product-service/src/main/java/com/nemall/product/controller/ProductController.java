package com.nemall.product.controller;

import com.nemall.common.response.Result;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.ProductCardVO;
import com.nemall.product.vo.ProductPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * 商品列表 HTTP 接口，将业务层返回的商品卡片封装为统一响应。
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 处理 {@code GET /api/products}，默认返回销量最高的前 10 件商品。
     *
     * @param sort 排序方式，默认且当前仅支持 {@code sales_desc}
     * @param limit 最多返回的商品数量，默认 10，必须大于 0
     * @return 包含商品卡片列表的响应；无匹配商品时列表为空
     * @throws IllegalArgumentException 排序方式不受支持或查询数量不合法
     */
    @GetMapping
    public Result<List<ProductCardVO>> listProducts(
            @RequestParam(name = "sort", defaultValue = "sales_desc") String sort,
            @RequestParam(name = "limit", defaultValue = "8") Integer limit,
            @RequestParam(name = "categoryId", required = false) Long categoryId){

        List<ProductCardVO> products =
                productService.listProducts(sort, limit,categoryId);

        // 200 是响应体中的业务码，HTTP 状态由 Spring MVC 单独管理。
        Result<List<ProductCardVO>> result = new Result<>(200, "OK", products);


        return result;
    }

    @GetMapping("/page")
    public Result<ProductPageVO> listProductsByPage(
        @RequestParam(name = "categoryId", required = false)
        Long categoryId,
        @RequestParam(name = "pageNum", defaultValue = "1")
        Integer pageNum) {

            ProductPageVO pageVO =
                    productService.listProductsByPage(categoryId, pageNum);

            return new Result<>(200, "OK", pageVO);
    }

}
