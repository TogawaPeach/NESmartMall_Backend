package com.nemall.support.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.autoconfigure.reactive.ClientHttpConnectorBuilderCustomizer;
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public ClientHttpConnectorBuilderCustomizer<ReactorClientHttpConnectorBuilder>
    disableHttpRetry() {

        return builder -> builder.withHttpClientCustomizer(
                client -> client.disableRetry(true)
        );
    }

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder(
            ObjectProvider<WebClientCustomizer> customizers) {

        WebClient.Builder builder = WebClient.builder();

        // 保留 Spring Boot 对连接资源和数据编解码的默认配置
        customizers.orderedStream()
                .forEach(customizer -> customizer.customize(builder));

        return builder;
    }

    @Bean
    public WebClient agentWebClient(
            @LoadBalanced WebClient.Builder builder) {

        return builder
                .baseUrl("http://mall-agent-service")
                .build();
    }
}
