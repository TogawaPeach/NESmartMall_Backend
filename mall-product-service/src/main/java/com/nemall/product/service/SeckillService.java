package com.nemall.product.service;

import com.nemall.product.vo.ProductDetailVO;
import com.nemall.product.vo.SeckillProductVO;

import java.util.List;

public interface SeckillService {

    /** 查询秒杀专区轮播所需的商品列表。 */
    List<SeckillProductVO> listSeckillProducts();

    /** 查询已上架、未删除的秒杀商品详情，保留全部活动阶段。 */
    ProductDetailVO getSeckillProductDetail(Long id);
}
