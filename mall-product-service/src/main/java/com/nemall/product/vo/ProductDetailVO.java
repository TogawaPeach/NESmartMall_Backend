package com.nemall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailVO {

    private String id;

    private String name;

    private String model;

    private String coverUrl;

    // 价格单位：分
    private Long price;

    private Long sales;

    private Long stock;

    private List<String> detailImages;
}