package com.nemall.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.nemall.api.client.ProductClient;
import com.nemall.api.dto.ProductBatchQueryDTO;
import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.api.dto.PurchaseItemsDTO;
import com.nemall.common.response.Result;
import com.nemall.order.dto.CreateSeckillOrderDTO;
import com.nemall.order.dto.SeckillOrderMessage;
import com.nemall.order.exception.OrderBusinessException;
import com.nemall.order.mapper.OrderItemMapper;
import com.nemall.order.mapper.OrderMapper;
import com.nemall.order.service.SeckillOrderCreateService;
import com.nemall.order.support.OrderNoGenerator;
import com.nemall.order.vo.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillOrderCreateServiceImpl implements SeckillOrderCreateService {

    private final OrderNoGenerator orderNoGenerator;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;

    @Override
    @GlobalTransactional(
            name = "create-seckill-order",
            rollbackFor = Exception.class
    )
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(SeckillOrderMessage message) {

        String orderNo = orderNoGenerator.next();

        // 查询商品名称、价格等信息，作为订单成交快照。
        ProductSummaryDTO product = queryProduct(message.productId());

        OrderDetailVO order = buildOrder(
                orderNo,
                product,
                message.recipient()
        );

        // 下单人取自 MQ 消息。
        orderMapper.insert(message.userId(), order);

        // 每次固定购买一件。
        orderItemMapper.insertSeckillItem(order.getId(), product);

        // 调用商品微服务，扣减一件数据库库存。
        deductStock(product.getId(), 1);
    }

    private ProductSummaryDTO queryProduct(String productId) {
        ProductBatchQueryDTO request = new ProductBatchQueryDTO();
        request.setIds(List.of(Long.valueOf(productId)));

        Result<List<ProductSummaryDTO>> result =
                productClient.queryByIds(request);

        if (result == null
                || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null
                || result.getData().isEmpty()) {
            throw new OrderBusinessException(400, "下单失败");
        }

        ProductSummaryDTO product = result.getData().getFirst();

        if (product.getPrice() == null || product.getPrice() < 0) {
            throw new OrderBusinessException(400, "下单失败");
        }

        return product;
    }

    private OrderDetailVO buildOrder(
            String orderNo,
            ProductSummaryDTO product,
            CreateSeckillOrderDTO.Recipient recipient) {
        OrderDetailVO order = new OrderDetailVO();

        order.setOrderNo(orderNo);
        order.setStatus("PENDING_PAYMENT");

        // 固定购买一件，商品金额等于单价，运费为零。
        order.setGoodsAmountFen(product.getPrice());
        order.setShippingFeeFen(0L);
        order.setPayableAmountFen(product.getPrice());

        order.setCreatedAt(
                Instant.now().truncatedTo(ChronoUnit.MILLIS)
        );

        order.setRecipient(BeanUtil.toBean(
                recipient,
                OrderDetailVO.RecipientVO.class
        ));

        return order;
    }

    private void deductStock(Long productId, int quantity) {
        PurchaseItemsDTO.Item item = new PurchaseItemsDTO.Item();
        item.setProductId(productId);
        item.setQuantity(quantity);

        PurchaseItemsDTO request = new PurchaseItemsDTO();
        request.setItems(List.of(item));

        Result<List<ProductSummaryDTO>> result =
                productClient.deductStock(request);

        if (result == null
                || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null
                || result.getData().isEmpty()) {
            throw new OrderBusinessException(400, "下单失败");
        }
    }
}
