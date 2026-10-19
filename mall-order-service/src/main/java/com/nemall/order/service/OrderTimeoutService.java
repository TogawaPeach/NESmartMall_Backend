package com.nemall.order.service;

/** MQ 超时关单业务。 */
public interface OrderTimeoutService {

    /** 仅取消已到期的待付款订单，并归还本单库存。 */
    void cancelExpiredOrder(String orderNo);
}
