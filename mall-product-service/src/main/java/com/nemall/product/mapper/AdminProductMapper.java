package com.nemall.product.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 商家端商品管理 Mapper。 */
public interface AdminProductMapper {

    /** 修改未删除商品的上下架状态。 */
    @Update("""
            UPDATE product
            SET status = #{status}
            WHERE id = #{id}
              AND is_deleted = 0
            """)
    int updateProductStatus(
            @Param("id") Long id,
            @Param("status") Integer status);
}
