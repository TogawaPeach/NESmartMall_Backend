package com.nemall.order.mapper;

import com.nemall.order.vo.OrderDetailVO;
import com.nemall.order.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    List<OrderVO> selectOrders(
            @Param("userId") Long userId,
            @Param("statusCode") Integer statusCode);

    /** 按订单号查询详情，始终限定当前用户。 */
    OrderDetailVO selectByOrderNo(
            @Param("userId") Long userId,
            @Param("orderNo") String orderNo);
}
