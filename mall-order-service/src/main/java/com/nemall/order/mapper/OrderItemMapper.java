package com.nemall.order.mapper;

import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    /** 一条 SQL 批量保存本次订单的商品成交快照。 */
    int insertBatch(@Param("items") List<OrderItem> items);

    /** 直接绑定商品查询结果，保存一件秒杀商品的成交快照。 */
    int insertSeckillItem(
            @Param("orderId") Long orderId,
            @Param("product") ProductSummaryDTO product);

    List<OrderItem> selectByOrderIds(
            @Param("orderIds") List<Long> orderIds);
}
