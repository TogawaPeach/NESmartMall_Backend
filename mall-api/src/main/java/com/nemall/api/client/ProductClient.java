package com.nemall.api.client;

import com.nemall.api.dto.ProductBatchQueryDTO;
import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.api.dto.PurchaseItemsDTO;
import com.nemall.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "mall-product-service")
public interface ProductClient {

    /** 仅由订单取消事务调用，按订单购买数量归还库存。 */
    @PostMapping("/internal/products/stock/restore")
    Result<Boolean> restoreStock(@RequestBody PurchaseItemsDTO request);

    /** 批量扣库存，返回本次成交使用的商品快照。 */
    @PostMapping("/internal/products/stock/deduct")
    Result<List<ProductSummaryDTO>> deductStock(
            @RequestBody PurchaseItemsDTO request);

    @PostMapping("/internal/products/batch")
    Result<List<ProductSummaryDTO>> queryByIds(
            @RequestBody ProductBatchQueryDTO request);
}
