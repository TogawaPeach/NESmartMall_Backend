package com.nemall.product.entity;

import lombok.Data;

import java.time.LocalDateTime;

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

    private Boolean isSeckill;

    // 数据库 DATETIME 不包含时区，返回 VO 时再按约定时区转换。
    private LocalDateTime seckillStartTime;

    private LocalDateTime seckillEndTime;
}
