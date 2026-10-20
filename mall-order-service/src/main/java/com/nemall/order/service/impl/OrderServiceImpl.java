package com.nemall.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.nemall.api.client.ProductClient;
import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.api.dto.PurchaseItemsDTO;
import com.nemall.common.response.Result;
import com.nemall.common.security.UserContext;
import com.nemall.order.dto.CreateOrderDTO;
import com.nemall.order.entity.OrderItem;
import com.nemall.order.exception.OrderBusinessException;
import com.nemall.order.mapper.OrderItemMapper;
import com.nemall.order.mapper.OrderMapper;
import com.nemall.order.service.OrderMessageService;
import com.nemall.order.service.OrderService;
import com.nemall.order.support.OrderNoGenerator;
import com.nemall.order.vo.OrderDetailVO;
import com.nemall.order.vo.OrderItemVO;
import com.nemall.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;
    private final OrderNoGenerator orderNoGenerator;
    private final OrderMessageService orderMessageService;

    /**
     * 创建待付款订单并扣减库存，不在此处执行支付。
     */
    @Override
    @GlobalTransactional(
            name = "create-order",
            rollbackFor = Exception.class
    )
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO createOrder(CreateOrderDTO request) {

        Long userId = UserContext.getUserId();
        String orderNo = orderNoGenerator.next();

        // 转换商品 ID、拒绝重复商品，并按商品 ID 排序。
        PurchaseItemsDTO purchase = buildPurchaseItems(request.getItems());

        // 商品服务条件扣库存，并返回本次成交使用的权威商品快照。
        List<ProductSummaryDTO> products = requireData(
                productClient.deductStock(purchase)
        );

        Map<Long, Integer> quantities = purchase.getItems().stream()
                .collect(Collectors.toMap(
                        PurchaseItemsDTO.Item::getProductId,
                        PurchaseItemsDTO.Item::getQuantity
                ));

        List<OrderItem> items = products.stream()
                .map(product -> {
                    OrderItem item = BeanUtil.toBean(
                            product,
                            OrderItem.class,
                            CopyOptions.create().setFieldMapping(Map.of(
                                    "id", "productId",
                                    "name", "productName",
                                    "model", "productModel",
                                    "coverUrl", "coverImageUrl",
                                    "price", "unitPriceFen"
                            ))
                    );

                    item.setQuantity(quantities.get(product.getId()));
                    return item;
                })
                .toList();

        long goodsAmountFen = calculateGoodsAmount(items);
        long shippingFeeFen = 0L;

        Instant createdAt = Instant.now()
                .truncatedTo(ChronoUnit.MILLIS);

        OrderDetailVO order = new OrderDetailVO();
        order.setOrderNo(orderNo);
        order.setStatus("PENDING_PAYMENT");
        order.setGoodsAmountFen(goodsAmountFen);
        order.setShippingFeeFen(shippingFeeFen);
        order.setPayableAmountFen(
                Math.addExact(goodsAmountFen, shippingFeeFen)
        );
        order.setCreatedAt(createdAt);
        order.setRecipient(BeanUtil.toBean(
                request.getRecipient(),
                OrderDetailVO.RecipientVO.class
        ));

        // MyBatis 回填订单自增主键。
        orderMapper.insert(userId, order);

        items.forEach(item -> item.setOrderId(order.getId()));
        orderItemMapper.insertBatch(items);

        // 全局提交成功后直接发 MQ，不保存消息任务或购物车快照。
        orderMessageService.sendAfterGlobalCommit(userId, order, items);

        return getOrderDetail(orderNo);
    }

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

        // 支付期限固定为创建后 15 分钟，仅计算响应字段，不增加数据库列。
        detail.setPayExpireAt(detail.getCreatedAt().plusSeconds(15 * 60));
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

    @Override
    public void confirmReceipt(String orderNo) {
        Long userId = UserContext.getUserId();
        orderMapper.confirmReceipt(userId, orderNo);
    }

    private <T> T requireData(Result<T> result) {

        if (result == null
                || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null) {
            throw new IllegalStateException("内部服务调用失败");
        }

        return result.getData();
    }

    private long calculateGoodsAmount(List<OrderItem> items) {

        long total = 0L;

        try {
            for (OrderItem item : items) {
                long itemAmount = Math.multiplyExact(
                        item.getUnitPriceFen(),
                        item.getQuantity().longValue()
                );

                total = Math.addExact(total, itemAmount);
            }
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "订单金额计算溢出", exception
            );
        }

        if (total > 9_007_199_254_740_991L) {
            throw new IllegalStateException("订单金额超出支持范围");
        }

        return total;
    }

    private PurchaseItemsDTO buildPurchaseItems(
            List<CreateOrderDTO.Item> sourceItems) {

        List<PurchaseItemsDTO.Item> items;

        try {
            items = sourceItems.stream()
                    .map(source -> {
                        PurchaseItemsDTO.Item item = BeanUtil.toBean(
                                source,
                                PurchaseItemsDTO.Item.class,
                                CopyOptions.create()
                                        .setIgnoreProperties("productId")
                        );

                        // 显式转换，超过 Long 范围时直接拒绝。
                        item.setProductId(
                                Long.valueOf(source.getProductId())
                        );

                        return item;
                    })
                    .sorted(Comparator.comparing(
                            PurchaseItemsDTO.Item::getProductId
                    ))
                    .toList();
        } catch (NumberFormatException exception) {
            throw new OrderBusinessException(
                    400, "商品ID超出有效范围"
            );
        }

        long productCount = items.stream()
                .map(PurchaseItemsDTO.Item::getProductId)
                .distinct()
                .count();

        if (productCount != items.size()) {
            throw new OrderBusinessException(
                    400, "商品重复，请刷新购物车后下单"
            );
        }

        PurchaseItemsDTO purchase = new PurchaseItemsDTO();
        purchase.setItems(items);

        return purchase;
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
