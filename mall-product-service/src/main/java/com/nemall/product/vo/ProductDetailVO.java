package com.nemall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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

    // null 表示当前查询尚未填充秒杀信息。
    private Boolean isSeckill;

    private LocalDateTime seckillStartTime;

    private LocalDateTime seckillEndTime;

    /** 保留普通商品查询使用的构造方法，新增秒杀字段默认不赋值。 */
    public ProductDetailVO(String id, String name, String model,
                           String coverUrl, Long price, Long sales,
                           Long stock, List<String> detailImages) {
        this(id, name, model, coverUrl, price, sales, stock,
                detailImages, null, null, null);
    }
}
