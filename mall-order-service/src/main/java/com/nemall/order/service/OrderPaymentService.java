package com.nemall.order.service;

/** 订单模拟支付，仅更新订单数据库。 */
public interface OrderPaymentService {

    void pay(String orderNo);
}
