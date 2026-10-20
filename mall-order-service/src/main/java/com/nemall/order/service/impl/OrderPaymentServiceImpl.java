package com.nemall.order.service.impl;

import com.nemall.common.security.UserContext;
import com.nemall.order.exception.OrderBusinessException;
import com.nemall.order.mapper.OrderMapper;
import com.nemall.order.service.OrderPaymentService;
import com.nemall.order.service.OrderService;
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
    public void pay(String orderNo) {

        Long userId = UserContext.getUserId();

        // 先取得行锁，再用下一条 SQL 的数据库时间判断期限。
        if (orderMapper.lockForPayment(userId, orderNo) == null) {
            throw new OrderBusinessException(
                    404, "订单不存在或无法访问"
            );
        }

        // 调用 Mapper，将待付款订单改为待发货。
        int rows = orderMapper.pay(userId, orderNo);
        if (rows == 1) {
            return;
        }

        String status = orderService.getOrderDetail(orderNo).getStatus();

        // 已付款的订单重复请求，按成功处理。
        if (PAID_STATUSES.contains(status)) {
            return;
        }

        if ("CANCELLED".equals(status)) {
            throw new OrderBusinessException(409, "订单已取消");
        }

        throw new OrderBusinessException(409, "订单支付已超时");
    }
}
