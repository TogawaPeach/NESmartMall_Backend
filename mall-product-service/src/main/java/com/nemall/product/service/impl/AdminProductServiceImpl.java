package com.nemall.product.service.impl;

import com.nemall.product.mapper.AdminProductMapper;
import com.nemall.product.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 商家端商品管理业务实现。 */
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final AdminProductMapper adminProductMapper;

    @Override
    public void updateProductStatus(Long id, Integer status) {
        adminProductMapper.updateProductStatus(id, status);
    }
}
