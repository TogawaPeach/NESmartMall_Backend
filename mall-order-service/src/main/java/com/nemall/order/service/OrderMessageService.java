package com.nemall.order.service;

import com.nemall.order.entity.OrderItem;
import com.nemall.order.vo.OrderDetailVO;

import java.util.List;

/** 订单全局提交后的消息发送。 */
public interface OrderMessageService {

    void sendAfterGlobalCommit(
            Long userId, OrderDetailVO order, List<OrderItem> items);
}
