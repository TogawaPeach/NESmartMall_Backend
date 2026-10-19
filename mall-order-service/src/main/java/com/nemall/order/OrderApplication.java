package com.nemall.order;

import com.nemall.api.client.ProductClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration.HttpClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * 订单服务启动入口，只扫描本服务根包下的 Spring 组件。
 */
@SpringBootApplication
@EnableFeignClients(clients = ProductClient.class)
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @Bean
    public HttpClientBuilderCustomizer disableHttpClientRetries() {
        return builder -> builder.disableAutomaticRetries();
    }
}
