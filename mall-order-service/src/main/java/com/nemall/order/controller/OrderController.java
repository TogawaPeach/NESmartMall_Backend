package com.nemall.order.controller;

import com.nemall.common.response.Result;
import com.nemall.order.dto.CreateOrderDTO;
import com.nemall.order.dto.CreateSeckillOrderDTO;
import com.nemall.order.service.OrderPaymentService;
import com.nemall.order.service.OrderService;
import com.nemall.order.service.OrderTimeoutService;
import com.nemall.order.service.SeckillOrderService;
import com.nemall.order.vo.OrderDetailVO;
import com.nemall.order.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderPaymentService orderPaymentService;
    private final OrderTimeoutService orderTimeoutService;
    private final SeckillOrderService seckillOrderService;

    /** 提交秒杀购买请求，每次固定购买一件。 */
    @PostMapping("/seckill")
    public Result<Void> createSeckillOrder(
            @RequestBody CreateSeckillOrderDTO request) throws Exception {

        seckillOrderService.submit(request);

        return new Result<>(200, "购买成功", null);
    }

    /** 取消当前登录用户的待付款订单。 */
    @PostMapping("/{orderNo}/cancel")
    public Result<Map<String, Object>> cancel(
            @PathVariable("orderNo") String orderNo) {

        OrderDetailVO order = orderTimeoutService.cancelOrder(orderNo);

        return new Result<>(200, "OK", Map.of(
                "orderNo", order.getOrderNo(),
                "status", order.getStatus()
        ));
    }

    /** 模拟支付当前用户的订单，不接入支付渠道。 */
    @PostMapping("/{orderNo}/pay")
    public Result<Void> pay(
            @PathVariable("orderNo") String orderNo) {

        orderPaymentService.pay(orderNo);

        return new Result<>(200, "OK", null);
    }

    /** 创建当前登录用户的待付款订单。 */
    @PostMapping
    public Result<OrderDetailVO> createOrder(
            @Valid @RequestBody CreateOrderDTO request) {

        OrderDetailVO detail = orderService.createOrder(request);

        return new Result<>(200, "OK", detail);
    }

    /** 查询当前登录用户的全部匹配订单，不分页。 */
    @GetMapping
    public Result<List<OrderVO>> getOrders(
            @RequestParam(value = "status", required = false)
            String status) {

        List<OrderVO> orders = orderService.getOrders(status);

        return new Result<>(200, "OK", orders);
    }

    @GetMapping("/{orderNo}")
    public Result<OrderDetailVO> getOrderDetail(
            @PathVariable("orderNo") String orderNo) {

        OrderDetailVO detail = orderService.getOrderDetail(orderNo);

        return new Result<>(200, "OK", detail);
    }

    /** 确认当前登录用户的订单收货。 */
    @PostMapping("/{orderNo}")
    public Result<Void> confirmReceipt(
            @PathVariable("orderNo") String orderNo) {

        orderService.confirmReceipt(orderNo);

        return new Result<>(200, "确认收货成功", null);
    }
}
