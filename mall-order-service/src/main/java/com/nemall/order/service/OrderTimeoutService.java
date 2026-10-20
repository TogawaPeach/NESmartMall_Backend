package com.nemall.order.service;

import com.nemall.order.vo.OrderDetailVO;

/** 用户主动取消、MQ 超时关单及库存回补业务。 */
public interface OrderTimeoutService {

    /** 取消当前用户的待付款订单；已取消订单按幂等成功返回。 */
    OrderDetailVO cancelOrder(String orderNo);

    /** 由 MQ 触发取消待付款订单；不存在或已结束的订单直接跳过。 */
    void cancelExpiredOrder(String orderNo);
}
