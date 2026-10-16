package com.nemall.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductBatchQueryDTO {

    /** 本次需要查询的商品 ID */
    private List<Long> ids;
}