package com.nemall.order.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** 一笔订单的卡片展示数据。 */
@Data
public class OrderVO {

    /** 仅供后端关联商品明细，不对外返回；前端使用 orderNo 标识订单。 */
    @JsonIgnore
    private String id;

    /** 对外订单号。 */
    private String orderNo;

    /** 英文订单状态。 */
    private String status;

    /** 下单时间。 */
    private Instant createdAt;

    /** 应付总金额，单位分。 */
    private Long payableAmountFen;

    /** 当前订单的商品成交快照。 */
    private List<OrderItemVO> items;
}
