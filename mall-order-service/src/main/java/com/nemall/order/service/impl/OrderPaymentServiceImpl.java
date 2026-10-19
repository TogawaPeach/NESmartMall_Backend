package com.nemall.order.service.impl;

import com.nemall.common.security.UserContext;
import com.nemall.order.exception.OrderBusinessException;
import com.nemall.order.mapper.OrderMapper;
import com.nemall.order.service.OrderPaymentService;
import com.nemall.order.service.OrderService;
import com.nemall.order.vo.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private static final Set<String> PAID_STATUSES = Set.of(
            "PENDING_SHIPMENT",
            "PENDING_RECEIPT",
            "COMPLETED"
    );

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO pay(String orderNo) {

        Long userId = UserContext.getUserId();

        // 先取得行锁，再用下一条 SQL 的数据库时间判断期限。
        Long orderId = orderMapper.lockForPayment(userId, orderNo);
        if (orderId == null) {
            throw new OrderBusinessException(
                    404, "订单不存在或无法访问"
            );
        }

        int affectedRows = orderMapper.pay(userId, orderNo);
        OrderDetailVO detail = orderService.getOrderDetail(orderNo);

        // 重复支付返回现有付款事实，不重复改状态、时间或扣库存。
        if (affectedRows == 1
                || PAID_STATUSES.contains(detail.getStatus())) {
            return detail;
        }

        if ("CANCELLED".equals(detail.getStatus())) {
            throw new OrderBusinessException(409, "订单已取消");
        }

        throw new OrderBusinessException(409, "订单支付已超时");
    }
}
