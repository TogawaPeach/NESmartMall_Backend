package com.nemall.order.service;

import com.nemall.order.vo.OrderDetailVO;

/** 订单模拟支付，仅更新订单数据库。 */
public interface OrderPaymentService {

    OrderDetailVO pay(String orderNo);
}
