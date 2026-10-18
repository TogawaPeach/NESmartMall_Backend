package com.nemall.order.entity;

import lombok.Data;

@Data
public class OrderItem {

    /** 订单明细 ID */
    private Long id;

    /** 所属订单 ID */
    private Long orderId;

    /** 原商品 ID */
    private Long productId;

    /** 商品名称快照 */
    private String productName;

    /** 商品型号快照 */
    private String productModel;

    /** 商品图片快照 */
    private String coverImageUrl;

    /** 成交单价，单位分 */
    private Long unitPriceFen;

    /** 购买数量 */
    private Integer quantity;
}
