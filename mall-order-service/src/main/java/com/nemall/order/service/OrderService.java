package com.nemall.order.service;

import com.nemall.order.dto.CreateOrderDTO;
import com.nemall.order.vo.OrderDetailVO;
import com.nemall.order.vo.OrderVO;

import java.util.List;

public interface OrderService {

    /** 创建当前登录用户的待付款订单。 */
    OrderDetailVO createOrder(CreateOrderDTO request);

    /**
     * 查询当前登录用户的订单，不分页。
     *
     * @param status 订单状态，null 表示全部状态
     * @return 订单列表，没有订单时返回空列表
     */
    List<OrderVO> getOrders(String status);

    /**
     * 按订单号查询当前登录用户的订单详情。
     *
     * @param orderNo 对外订单号
     * @return 完整订单详情；不存在或当前用户不可见时按 404 处理
     */
    OrderDetailVO getOrderDetail(String orderNo);

    /**
     * 确认当前登录用户的订单收货。
     *
     * @param orderNo 对外订单号
     */
    void confirmReceipt(String orderNo);
}
