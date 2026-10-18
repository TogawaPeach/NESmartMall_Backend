package com.nemall.order.mapper;

import com.nemall.order.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    List<OrderVO> selectOrders(
            @Param("userId") Long userId,
            @Param("statusCode") Integer statusCode);
}
