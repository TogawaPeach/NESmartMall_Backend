package com.nemall.product.service;

/** 商家端商品管理业务。 */
public interface AdminProductService {

    /** 修改商品上下架状态。 */
    void updateProductStatus(Long id, Integer status);
}
