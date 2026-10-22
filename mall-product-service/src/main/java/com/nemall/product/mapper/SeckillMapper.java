package com.nemall.product.mapper;

import com.nemall.product.vo.SeckillProductVO;

import java.util.List;

public interface SeckillMapper {

    /** 查询已上架、未删除的秒杀商品，按 ID 升序保留全部活动阶段。 */
    List<SeckillProductVO> selectSeckillProducts();
}
