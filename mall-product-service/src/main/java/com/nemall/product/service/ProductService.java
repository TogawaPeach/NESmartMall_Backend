package com.nemall.product.service;

import com.nemall.product.vo.ProductCardVO;

import java.util.List;

public interface ProductService {

    List<ProductCardVO> listProducts(String sort, Integer limit);
}