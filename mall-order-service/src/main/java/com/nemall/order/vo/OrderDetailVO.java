package com.nemall.order.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** 一笔订单的完整详情。 */
@Data
public class OrderDetailVO {

    /** 仅供后端关联商品明细，不对外返回。 */
    @JsonIgnore
    private Long id;

    /** 对外订单号。 */
    private String orderNo;

    /** 英文订单状态。 */
    private String status;

    private Instant createdAt;

    /** 由创建时间加 15 分钟计算，不单独入库。 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant payExpireAt;

    private Instant paidAt;
    private Instant shippedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    /** 商品成交快照列表。 */
    private List<OrderItemVO> items;

    /** 商品金额，单位分。 */
    private Long goodsAmountFen;

    /** 运费，单位分。 */
    private Long shippingFeeFen;

    /** 优惠金额，当前固定返回 0。 */
    private Long discountAmountFen;

    /** 应付金额，单位分。 */
    private Long payableAmountFen;

    /** 实付金额，单位分；未付款为 null。 */
    private Long paidAmountFen;

    /** 下单时的收货信息快照。 */
    private RecipientVO recipient;

    /** 物流信息；无运单号时为 null。 */
    private LogisticsVO logistics;

    @Data
    public static class RecipientVO {

        private String name;

        /** 入库时为完整联系电话，对外查询响应前脱敏。 */
        private String phone;

        private String address;
    }

    @Data
    public static class LogisticsVO {

        private String trackingNo;
    }
}
