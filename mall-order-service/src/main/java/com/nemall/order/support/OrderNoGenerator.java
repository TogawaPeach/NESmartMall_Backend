package com.nemall.order.support;

import cn.hutool.core.lang.Snowflake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderNoGenerator {

    private final Snowflake snowflake;

    public OrderNoGenerator(
            @Value("${mall.order.snowflake.worker-id}")
            long workerId,
            @Value("${mall.order.snowflake.datacenter-id}")
            long datacenterId) {

        // 默认起始时间、直接读取系统时间、不容忍时钟回拨。
        this.snowflake = new Snowflake(
                null, workerId, datacenterId, false, 0L
        );
    }

    public String next() {
        return "JW" + snowflake.nextIdStr();
    }
}
