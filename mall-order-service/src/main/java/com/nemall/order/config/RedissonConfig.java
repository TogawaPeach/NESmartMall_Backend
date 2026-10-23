package com.nemall.order.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${app.redis.address}") String address,
            @Value("${app.redis.password}") String password,
            @Value("${app.redis.database}") int database) {

        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);

        SingleServerConfig server = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database);

        if (StringUtils.hasText(password)) {
            server.setPassword(password);
        }

        return Redisson.create(config);
    }
}
