package com.nemall.product.mapper;

import com.nemall.product.dto.SaveProductDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 商家端商品管理 Mapper。 */
public interface AdminProductMapper {

    /** 新增商品，默认下架，销量和删除标记初始化为 0。 */
    @Insert("""
            INSERT INTO product (
                name, model, cover_image_url, detail_image_urls,
                category_id, price, stock,
                is_seckill, seckill_start_time, seckill_end_time,
                status, sales_count, is_deleted
            )
            VALUES (
                #{request.name},
                #{request.model},
                #{request.coverUrl,jdbcType=VARCHAR},
                COALESCE(#{request.detailImages,jdbcType=VARCHAR,typeHandler=com.nemall.product.handler.StringListJsonTypeHandler}, JSON_ARRAY()),
                #{request.categoryId},
                #{request.price},
                #{request.stock},
                #{request.isSeckill},
                #{request.seckillStartTime,jdbcType=TIMESTAMP},
                #{request.seckillEndTime,jdbcType=TIMESTAMP},
                2, 0, 0
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "request.id", keyColumn = "id")
    int insertProduct(@Param("request") SaveProductDTO request);

    /** 修改未删除商品，保留详情图、商品类型、上下架状态及销量。 */
    @Update("""
            UPDATE product
            SET name = #{request.name},
                model = #{request.model},
                cover_image_url = #{request.coverUrl,jdbcType=VARCHAR},
                category_id = #{request.categoryId},
                price = #{request.price},
                stock = #{request.stock},
                seckill_start_time = #{request.seckillStartTime,jdbcType=TIMESTAMP},
                seckill_end_time = #{request.seckillEndTime,jdbcType=TIMESTAMP}
            WHERE id = #{request.id}
              AND is_deleted = 0
            """)
    int updateProduct(@Param("request") SaveProductDTO request);

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
