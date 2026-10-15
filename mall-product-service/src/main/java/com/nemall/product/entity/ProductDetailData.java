package com.nemall.product.entity;

import lombok.Data;

@Data
public class ProductDetailData {

    private Long id;

    private String name;

    private String model;

    private String coverImageUrl;

    private Long price;

    private Long salesCount;

    private Long stock;

    // 接收数据库中的图片 JSON 文本
    private String detailImageUrls;
}