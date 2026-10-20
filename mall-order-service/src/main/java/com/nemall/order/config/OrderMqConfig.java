package com.nemall.order.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
public class OrderMqConfig {

    public static final String EXCHANGE = "mall.order.exchange";
    public static final String CART_QUEUE = "mall.cart.checkout.queue";
    public static final String CART_KEY = "cart.checkout";
    public static final String DELAY_QUEUE = "mall.order.payment.delay.queue";
    public static final String DELAY_KEY = "order.payment.delay";
    public static final String TIMEOUT_QUEUE = "mall.order.payment.timeout.queue";
    public static final String TIMEOUT_KEY = "order.payment.timeout";

    @Bean(destroyMethod = "close")
    public ExecutorService orderMessageExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public Declarables orderQueues() {
        DirectExchange exchange = new DirectExchange(EXCHANGE, true, false);
        Queue cart = QueueBuilder.durable(CART_QUEUE).build();
        // 此队列不设置消费者；消息过期后转入超时处理队列。
        Queue delay = QueueBuilder.durable(DELAY_QUEUE)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(TIMEOUT_KEY)
                .build();
        Queue timeout = QueueBuilder.durable(TIMEOUT_QUEUE).build();
        return new Declarables(
                exchange, cart, delay, timeout,
                BindingBuilder.bind(cart).to(exchange).with(CART_KEY),
                BindingBuilder.bind(delay).to(exchange).with(DELAY_KEY),
                BindingBuilder.bind(timeout).to(exchange).with(TIMEOUT_KEY)
        );
    }
}
