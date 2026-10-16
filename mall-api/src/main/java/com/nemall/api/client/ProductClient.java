package com.nemall.api.client;

import com.nemall.api.dto.ProductBatchQueryDTO;
import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "mall-product-service")
public interface ProductClient {

    @PostMapping("/internal/products/batch")
    Result<List<ProductSummaryDTO>> queryByIds(
            @RequestBody ProductBatchQueryDTO request);
}