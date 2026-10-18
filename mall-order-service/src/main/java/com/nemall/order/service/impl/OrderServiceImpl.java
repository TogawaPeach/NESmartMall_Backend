package com.nemall.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.nemall.common.security.UserContext;
import com.nemall.order.entity.OrderItem;
import com.nemall.order.exception.OrderBusinessException;
import com.nemall.order.mapper.OrderItemMapper;
import com.nemall.order.mapper.OrderMapper;
import com.nemall.order.service.OrderService;
import com.nemall.order.vo.OrderDetailVO;
import com.nemall.order.vo.OrderItemVO;
import com.nemall.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public List<OrderVO> getOrders(String status) {
        Long userId = UserContext.getUserId();
        Integer statusCode = parseStatus(status);

        // 第一次查询：当前用户的订单基本信息，按时间、ID 倒序，不分页。
        // Mapper 负责映射英文状态和 UTC 时间，此时 items 尚未填入。
        List<OrderVO> orders = orderMapper.selectOrders(userId, statusCode);
        if (orders.isEmpty()) {
            return orders;
        }

        List<Long> orderIds = orders.stream()
                .map(order -> Long.valueOf(order.getId()))
                .toList();

        // 第二次查询：一次读取这些订单的全部商品成交快照。
        List<OrderItem> items = orderItemMapper.selectByOrderIds(orderIds);

        Map<Long, List<OrderItem>> itemMap = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // 只在内存中组装商品列表，循环中不查询数据库。
        for (OrderVO order : orders) {
            Long orderId = Long.valueOf(order.getId());
            List<OrderItem> orderItems = itemMap.get(orderId);

            if (orderItems == null || orderItems.isEmpty()) {
                throw new IllegalStateException("订单商品明细缺失");
            }

            List<OrderItemVO> itemVOs = orderItems.stream()
                    .map(this::toItemVO)
                    .toList();

            order.setItems(itemVOs);
        }

        return orders;
    }

    @Override
    public OrderDetailVO getOrderDetail(String orderNo) {
        if (orderNo == null || !orderNo.matches("JW[1-9][0-9]{0,61}")) {
            throw new OrderBusinessException(400, "订单号不合法");
        }

        Long userId = UserContext.getUserId();

        // SQL 同时限定 order_no 和 user_id，直接映射完整详情。
        OrderDetailVO detail = orderMapper.selectByOrderNo(userId, orderNo);
        if (detail == null) {
            throw new OrderBusinessException(404, "订单不存在或无法访问");
        }

        if (detail.getStatus() == null) {
            throw new IllegalStateException("订单状态不合法");
        }

        // 复用现有商品明细查询，传入当前订单的内部主键。
        List<OrderItem> items =
                orderItemMapper.selectByOrderIds(List.of(detail.getId()));

        if (items.isEmpty()) {
            throw new IllegalStateException("订单商品明细缺失");
        }

        detail.setDiscountAmountFen(0L);

        detail.setItems(items.stream()
                .map(this::toItemVO)
                .toList());

        OrderDetailVO.RecipientVO recipient = detail.getRecipient();
        if (recipient == null) {
            throw new IllegalStateException("订单收货信息缺失");
        }
        recipient.setPhone(maskPhone(recipient.getPhone()));

        return detail;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "已隐藏";
        }

        String value = phone.strip();
        if (value.length() <= 7) {
            return "已隐藏";
        }

        return value.substring(0, 3)
                + "*".repeat(value.length() - 7)
                + value.substring(value.length() - 4);
    }

    private Integer parseStatus(String status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case "PENDING_PAYMENT" -> 0;
            case "PENDING_SHIPMENT" -> 1;
            case "PENDING_RECEIPT" -> 2;
            case "COMPLETED" -> 3;
            case "CANCELLED" -> 4;
            default -> throw new IllegalArgumentException("订单状态不合法");
        };
    }

    private OrderItemVO toItemVO(OrderItem item) {
        return BeanUtil.toBean(
                item,
                OrderItemVO.class,
                CopyOptions.create().setFieldMapping(Map.of(
                        "productName", "name",
                        "productModel", "model",
                        "coverImageUrl", "coverUrl"
                ))
        );
    }
}
