package com.nemall.order.mapper;

import com.nemall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    List<OrderItem> selectByOrderIds(
            @Param("orderIds") List<Long> orderIds);
}
