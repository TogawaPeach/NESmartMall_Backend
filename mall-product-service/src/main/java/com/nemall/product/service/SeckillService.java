package com.nemall.product.service;

import com.nemall.product.vo.SeckillProductVO;

import java.util.List;

public interface SeckillService {

    /** 查询秒杀专区轮播所需的商品列表。 */
    List<SeckillProductVO> listSeckillProducts();
}
