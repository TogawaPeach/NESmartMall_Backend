package com.nemall.product;

import com.nemall.product.client.PythonProductClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 商品服务启动入口，扫描商品模块的 Spring 组件并注册 MyBatis Mapper 接口。
 */
@SpringBootApplication
@MapperScan("com.nemall.product.mapper")
@EnableFeignClients(clients = PythonProductClient.class)
public class ProductApplication {

    /**
     * 启动商品服务；本机可通过 {@code --spring.profiles.active=local} 加载本地数据库配置。
     *
     * @param args Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }

}
