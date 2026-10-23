package com.nemall.order.service;

import com.nemall.order.dto.SeckillOrderMessage;

/** 秒杀消息的订单落库入口，由独立事务服务实现。 */
public interface SeckillOrderCreateService {

    void createOrder(SeckillOrderMessage message);
}
