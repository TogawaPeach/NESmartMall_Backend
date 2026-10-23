package com.nemall.order.listener;

import com.nemall.order.config.OrderMqConfig;
import com.nemall.order.dto.SeckillOrderMessage;
import com.nemall.order.service.SeckillOrderCreateService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SeckillOrderListener {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final SeckillOrderCreateService seckillOrderCreateService;

    public SeckillOrderListener(
            SeckillOrderCreateService seckillOrderCreateService) {
        this.seckillOrderCreateService = seckillOrderCreateService;
    }

    // 每个实例固定一个消费者，配合 prefetch=1，处理完成并确认后再接收下一条。
    @RabbitListener(queues = OrderMqConfig.SECKILL_QUEUE, concurrency = "1")
    public void handle(Message message) {
        SeckillOrderMessage payload = JSON.readValue(
                message.getBody(), SeckillOrderMessage.class
        );

        // 经 Spring 代理调用独立的落库事务服务。
        seckillOrderCreateService.createOrder(payload);
    }
}
