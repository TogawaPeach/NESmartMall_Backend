package com.nemall.product.dto;

import lombok.Data;

/** 商家商品上下架请求，1 表示上架，2 表示下架。 */
@Data
public class UpdateProductStatusDTO {

    private Integer status;
}
