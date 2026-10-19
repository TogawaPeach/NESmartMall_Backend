package com.nemall.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.nemall.api.client.ProductClient;
import com.nemall.api.dto.PurchaseItemsDTO;
import com.nemall.common.response.Result;
import com.nemall.order.entity.OrderItem;
import com.nemall.order.mapper.OrderItemMapper;
import com.nemall.order.mapper.OrderMapper;
import com.nemall.order.service.OrderTimeoutService;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** MQ 超时取消与库存回补。 */
@Service
@RequiredArgsConstructor
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    @GlobalTransactional(
            name = "cancel-expired-order",
            rollbackFor = Exception.class
    )
    public void cancelExpiredOrder(String orderNo) {

        transactionTemplate.executeWithoutResult(transaction -> {

            // 先竞争订单状态，只有成功取消的一次处理才能回补库存。
            if (orderMapper.cancelExpired(orderNo) == 0) {
                if (orderMapper.isPendingPayment(orderNo)) {
                    // 消息提前到达时不能直接确认，否则将失去后续关单机会。
                    throw new IllegalStateException("订单尚未到期，稍后重试关单");
                }
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
