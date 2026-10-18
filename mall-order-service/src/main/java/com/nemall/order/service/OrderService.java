package com.nemall.order.service;

import com.nemall.order.vo.OrderVO;

import java.util.List;

public interface OrderService {

    /**
     * 查询当前登录用户的订单，不分页。
     *
     * @param status 订单状态，null 表示全部状态
     * @return 订单列表，没有订单时返回空列表
     */
    List<OrderVO> getOrders(String status);
}