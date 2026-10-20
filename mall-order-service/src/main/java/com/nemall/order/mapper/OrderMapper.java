package com.nemall.order.mapper;

import com.nemall.order.vo.OrderDetailVO;
import com.nemall.order.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    /** 先锁定当前用户的订单，付款资格随后用数据库时间判断。 */
    Long lockForPayment(
            @Param("userId") Long userId,
            @Param("orderNo") String orderNo);

    /** 仅把当前用户创建未满 15 分钟的待付款订单改为待发货。 */
    int pay(
            @Param("userId") Long userId,
            @Param("orderNo") String orderNo);

    /** 由延迟消息触发，仅取消待付款订单；到期时机由 MQ 控制。 */
    int cancelExpired(@Param("orderNo") String orderNo);

    /** 内部关单后关联本单的商品成交快照。 */
    Long selectIdByOrderNo(@Param("orderNo") String orderNo);

    /** 插入待付款订单，回填 order.id；用户 ID 来自可信上下文。 */
    int insert(
            @Param("userId") Long userId,
            @Param("order") OrderDetailVO order);

    List<OrderVO> selectOrders(
            @Param("userId") Long userId,
            @Param("statusCode") Integer statusCode);

    /** 按订单号查询详情，始终限定当前用户。 */
    OrderDetailVO selectByOrderNo(
            @Param("userId") Long userId,
            @Param("orderNo") String orderNo);

    /** 仅更新当前用户的待收货订单。 */
    int confirmReceipt(
            @Param("userId") Long userId,
            @Param("orderNo") String orderNo);
}
