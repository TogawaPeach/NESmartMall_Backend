package com.nemall.order.mapper;

import com.nemall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    /** 一条 SQL 批量保存本次订单的商品成交快照。 */
    int insertBatch(@Param("items") List<OrderItem> items);

    List<OrderItem> selectByOrderIds(
            @Param("orderIds") List<Long> orderIds);
}
