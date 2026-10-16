package com.nemall.cart;

import com.nemall.api.client.ProductClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 购物车服务启动入口，只扫描本服务根包下的 Spring 组件。
 */
@SpringBootApplication
@EnableFeignClients(clients = ProductClient.class)
public class CartApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}
