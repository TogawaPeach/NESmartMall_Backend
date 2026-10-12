package com.nemall.product.mapper;

import com.nemall.product.vo.ProductCardVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品查询 Mapper，SQL 和字段映射定义在 {@code mapper/ProductMapper.xml} 中。
 */
public interface ProductMapper {

    /**
     * 查询销量最高的商品卡片，仅包含未删除且已上架（status=1）的商品。
     *
     * @param limit 查询上限，由业务层保证大于 0；名称对应 XML 中的绑定参数
     * @return 按销量降序、商品 ID 升序排列的商品卡片列表
     */
    List<ProductCardVO> selectTopSales(@Param("limit") Integer limit,
                                       @Param("categoryId") Long categoryId);

}
