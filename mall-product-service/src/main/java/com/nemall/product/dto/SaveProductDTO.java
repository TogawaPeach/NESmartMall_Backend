package com.nemall.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaveProductDTO {

    private Long id;

    private String name;

    private String model;

    private String coverUrl;

    @JsonSetter(nulls = Nulls.FAIL)
    private List<String> detailImages;

    private Long categoryId;

    // 单位：分
    private Long price;

    private Integer stock;

    @JsonProperty("isSeckill")
    private Boolean isSeckill;

    private LocalDateTime seckillStartTime;

    private LocalDateTime seckillEndTime;
}
