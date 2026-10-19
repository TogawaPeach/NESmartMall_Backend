package com.nemall.order.config;

import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecovererWithConfirms;
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
    public static final String FAILED_QUEUE = "mall.order.payment.failed.queue";
    public static final String FAILED_KEY = "order.payment.failed";

    @Bean(destroyMethod = "close")
    public ExecutorService orderMessageExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public MessageRecoverer orderTimeoutRecoverer(RabbitTemplate rabbitTemplate) {
        RepublishMessageRecovererWithConfirms recoverer =
                new RepublishMessageRecovererWithConfirms(
                        rabbitTemplate, EXCHANGE, FAILED_KEY,
                        ConfirmType.CORRELATED
                );
        recoverer.setConfirmTimeout(3_000);

        return (message, cause) -> {
            // 失败消息不设 TTL 等待处理，不沿用原来的过期时间。
            message.getMessageProperties().setExpiration(null);
            try {
                recoverer.recover(message, cause);
            } catch (Exception exception) {
                // 未确认进入失败队列时保留原消息，不能确认或直接丢弃。
                throw new ImmediateRequeueAmqpException(
                        "超时消息转入失败队列未确认，保留原消息", exception
                );
            }
        };
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
        // 无消费者、无 TTL，保留失败消息供后续人工重放。
        Queue failed = QueueBuilder.durable(FAILED_QUEUE).build();

        return new Declarables(
                exchange, cart, delay, timeout, failed,
                BindingBuilder.bind(cart).to(exchange).with(CART_KEY),
                BindingBuilder.bind(delay).to(exchange).with(DELAY_KEY),
                BindingBuilder.bind(timeout).to(exchange).with(TIMEOUT_KEY),
                BindingBuilder.bind(failed).to(exchange).with(FAILED_KEY)
        );
    }
}
