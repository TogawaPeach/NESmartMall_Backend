package com.nemall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillProductVO {

    private String id;

    private String name;

    private String model;

    private String coverUrl;

    // 价格单位：分
    private Long price;

    private LocalDateTime seckillStartTime;

    private LocalDateTime seckillEndTime;

    private Boolean isSeckill;
}
