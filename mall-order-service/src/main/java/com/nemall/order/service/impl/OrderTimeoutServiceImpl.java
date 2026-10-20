package com.nemall.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.nemall.api.client.ProductClient;
import com.nemall.api.dto.PurchaseItemsDTO;
import com.nemall.common.response.Result;
import com.nemall.common.security.UserContext;
import com.nemall.order.entity.OrderItem;
import com.nemall.order.exception.OrderBusinessException;
import com.nemall.order.mapper.OrderItemMapper;
import com.nemall.order.mapper.OrderMapper;
import com.nemall.order.service.OrderTimeoutService;
import com.nemall.order.vo.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** 用户主动取消、MQ 超时关单及库存回补。 */
@Service
@RequiredArgsConstructor
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    @GlobalTransactional(
            name = "cancel-order",
            rollbackFor = Exception.class
    )
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO cancelOrder(String orderNo) {

        if (orderNo == null
                || !orderNo.matches("JW[1-9][0-9]{0,61}")) {
            throw new OrderBusinessException(400, "订单号格式错误");
        }

        Long userId = UserContext.getUserId();

        // 按当前用户锁定订单，避免与支付、MQ 取消并发修改。
        Long orderId = orderMapper.lockForPayment(userId, orderNo);
        if (orderId == null) {
            throw new OrderBusinessException(404, "订单不存在");
        }

        // 复用现有取消及库存回补逻辑。
        cancelExpiredOrder(orderNo);

        // 现有取消方法更新 0 行会跳过，需要确认实际状态。
        OrderDetailVO order =
                orderMapper.selectByOrderNo(userId, orderNo);

        if (order == null) {
            throw new OrderBusinessException(404, "订单不存在");
        }

        // 本次取消或此前已经取消，均返回成功。
        if ("CANCELLED".equals(order.getStatus())) {
            return order;
        }

        if ("PENDING_SHIPMENT".equals(order.getStatus())) {
            throw new OrderBusinessException(
                    409, "订单已付款，无法取消"
            );
        }

        if ("PENDING_PAYMENT".equals(order.getStatus())) {
            throw new IllegalStateException("待付款订单未成功取消");
        }

        throw new OrderBusinessException(
                409, "当前订单状态不允许取消"
        );
    }

    @Override
    @GlobalTransactional(
            name = "cancel-expired-order",
            rollbackFor = Exception.class
    )
    public void cancelExpiredOrder(String orderNo) {

        transactionTemplate.executeWithoutResult(transaction -> {

            // 先竞争订单状态，只有成功取消的一次处理才能回补库存。
            if (orderMapper.cancelExpired(orderNo) == 0) {
                return;
            }

            Long orderId = orderMapper.selectIdByOrderNo(orderNo);
            List<OrderItem> items =
                    orderItemMapper.selectByOrderIds(List.of(orderId));

            if (items.isEmpty()) {
                throw new IllegalStateException("订单商品明细缺失");
            }

            PurchaseItemsDTO request = new PurchaseItemsDTO();
            request.setItems(items.stream()
                    .map(item -> BeanUtil.toBean(
                            item, PurchaseItemsDTO.Item.class
                    ))
                    .toList());

            Result<Boolean> result = productClient.restoreStock(request);
            if (result == null
                    || !Integer.valueOf(200).equals(result.getCode())
                    || !Boolean.TRUE.equals(result.getData())) {
                throw new IllegalStateException("库存回补失败");
            }
        });
    }
}
