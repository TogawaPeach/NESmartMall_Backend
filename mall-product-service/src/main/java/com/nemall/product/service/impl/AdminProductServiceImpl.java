package com.nemall.product.service.impl;

import com.nemall.product.cache.ProductBloomFilter;
import com.nemall.product.client.PythonProductClient;
import com.nemall.product.dto.ProductKnowledgeRequest;
import com.nemall.product.dto.SaveProductDTO;
import com.nemall.product.mapper.AdminProductMapper;
import com.nemall.product.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 商家端商品管理业务实现。 */
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final AdminProductMapper adminProductMapper;
    private final RedissonClient redissonClient;
    private final ProductBloomFilter productBloomFilter;
    private final PythonProductClient pythonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProduct(SaveProductDTO request) {
        if (request.getId() == null) {
            adminProductMapper.insertProduct(request);
            productBloomFilter.add(request.getId());
        } else {
            adminProductMapper.updateProduct(request);
            redissonClient.getBucket("product:detail:" + request.getId()).delete();
        }

        ProductKnowledgeRequest knowledgeRequest = new ProductKnowledgeRequest(
                request.getId().toString(),
                request.getName(),
                request.getModel(),
                request.getCoverUrl(),
                request.getPrice()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        pythonClient.submitProduct(knowledgeRequest);
                    }
                }
        );
    }

    @Override
    public void updateProductStatus(Long id, Integer status) {
        redissonClient.getBucket("product:detail:" + id).delete();
        adminProductMapper.updateProductStatus(id, status);
    }
}
