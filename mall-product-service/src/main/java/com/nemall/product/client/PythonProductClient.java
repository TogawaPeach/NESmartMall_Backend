package com.nemall.product.client;

import com.nemall.product.dto.ProductKnowledgeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mall-agent-service")
public interface PythonProductClient {

    @PostMapping("/internal/guide/products")
    void submitProduct(@RequestBody ProductKnowledgeRequest request);
}
