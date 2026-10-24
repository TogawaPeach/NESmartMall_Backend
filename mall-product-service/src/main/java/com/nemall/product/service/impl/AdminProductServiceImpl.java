package com.nemall.product.service.impl;

import com.nemall.product.dto.SaveProductDTO;
import com.nemall.product.mapper.AdminProductMapper;
import com.nemall.product.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 商家端商品管理业务实现。 */
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final AdminProductMapper adminProductMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public void saveProduct(SaveProductDTO request) {
        if (request.getId() == null) {
            adminProductMapper.insertProduct(request);
        } else {
            adminProductMapper.updateProduct(request);
            redissonClient.getBucket("product:detail:" + request.getId()).delete();
        }
    }

    @Override
    public void updateProductStatus(Long id, Integer status) {
        redissonClient.getBucket("product:detail:" + id).delete();
        adminProductMapper.updateProductStatus(id, status);
    }
}
