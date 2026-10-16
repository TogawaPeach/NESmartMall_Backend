package com.nemall.api.dto;

import lombok.Data;

@Data
public class ProductSummaryDTO {

    /** 商品 ID */
    private Long id;

    /** 商品名称 */
    private String name;

    /** 型号 */
    private String model;

    /** 封面图片地址 */
    private String coverUrl;

    /** 单价，单位：分；null 表示价格待确认 */
    private Long price;

    /** 当前库存 */
    private Long stock;

    /** 商品状态：0 草稿，1 上架，2 下架 */
    private Integer status;

    /** 删除标记：0 正常，1 已删除 */
    private Integer isDeleted;
}