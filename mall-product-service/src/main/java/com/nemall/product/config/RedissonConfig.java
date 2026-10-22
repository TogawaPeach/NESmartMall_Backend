package com.nemall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJackson3Codec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${app.redis.address}") String address,
            @Value("${app.redis.password}") String password,
            @Value("${app.redis.database}") int database) {

        Config config = new Config();

        // Java 对象与 JSON 之间自动转换。
        config.setCodec(new JsonJackson3Codec());

        SingleServerConfig server = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database);

        if (StringUtils.hasText(password)) {
            server.setPassword(password);
        }

        return Redisson.create(config);
    }
}
