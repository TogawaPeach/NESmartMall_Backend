package com.nemall.order.vo;

import lombok.Data;

/** 订单中的一条商品成交快照。 */
@Data
public class OrderItemVO {

    /** 订单明细 ID。 */
    private String id;

    /** 原商品 ID。 */
    private String productId;

    /** 商品名称快照。 */
    private String name;

    /** 商品型号快照。 */
    private String model;

    /** 商品图片快照。 */
    private String coverUrl;

    /** 成交单价，单位分。 */
    private Long unitPriceFen;

    /** 购买数量。 */
    private Integer quantity;
}
