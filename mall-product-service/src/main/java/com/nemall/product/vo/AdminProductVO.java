package com.nemall.product.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家端商品列表中的单条商品。
 */
@Data
public class AdminProductVO {

    /** 字符串形式的商品 ID，避免前端精度丢失。 */
    private String id;

    private String coverUrl;

    private String name;

    private String model;

    /** 分类 ID：0 未分类，1～5 对应已有分类。 */
    private Long categoryId;

    /** 价格，单位：分；null 表示价格待确认。 */
    private Long price;

    private Long stock;

    /** 商品状态：0 草稿，1 上架，2 下架。 */
    private Integer status;

    private Boolean isSeckill;

    /** 北京时间，无时区；普通商品返回 null。 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime seckillStartTime;

    /** 北京时间，无时区；普通商品返回 null。 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime seckillEndTime;
}
