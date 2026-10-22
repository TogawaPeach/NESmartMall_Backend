package com.nemall.product.mapper;

import com.nemall.product.vo.ProductDetailVO;
import com.nemall.product.vo.SeckillProductVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SeckillMapper {

    /** 查询已上架、未删除的秒杀商品，按 ID 升序保留全部活动阶段。 */
    List<SeckillProductVO> selectSeckillProducts();

    /** 详情由 MyBatis 映射，不按秒杀起止时间过滤。 */
    ProductDetailVO selectSeckillProductDetailById(@Param("id") Long id);
}
