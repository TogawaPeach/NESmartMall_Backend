package com.nemall.order.controller;

import com.nemall.common.response.Result;
import com.nemall.order.service.OrderService;
import com.nemall.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 查询当前登录用户的全部匹配订单，不分页。 */
    @GetMapping
    public Result<List<OrderVO>> getOrders(
            @RequestParam(value = "status", required = false)
            String status) {

        List<OrderVO> orders = orderService.getOrders(status);

        return new Result<>(200, "OK", orders);
    }
}
