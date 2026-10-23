package com.nemall.order.service;

import com.nemall.order.dto.CreateSeckillOrderDTO;

public interface SeckillOrderService {

    /** 提交当前登录用户的秒杀购买请求，每次固定购买一件。 */
    void submit(CreateSeckillOrderDTO request) throws Exception;
}
