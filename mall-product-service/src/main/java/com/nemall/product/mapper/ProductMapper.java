package com.nemall.product.mapper;

import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.product.vo.AdminProductVO;
import com.nemall.product.vo.ProductCardVO;
import com.nemall.product.vo.ProductDetailVO;
import com.nemall.product.vo.ProductStatsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品查询 Mapper，SQL 和字段映射定义在 {@code mapper/ProductMapper.xml} 中。
 */
public interface ProductMapper {

    /** 查询全部商品 ID，供启动时预热布隆过滤器。 */
    List<Long> selectAllProductIds();

    /** 统计未删除商品的全部、在售和下架数量。 */
    ProductStatsVO selectProductStats();

    /** 按状态查询未删除商品，由 PageHelper 完成计数和分页。 */
    List<AdminProductVO> selectMerchantProducts(@Param("status") String status);

    /** 按订单原购买数量回补，不限制商品上架或逻辑删除状态。 */
    int restoreStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity);

    /** 仅扣减可售且库存充足的商品，返回影响行数。 */
    int deductStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity);

    /**
     * 查询销量最高的商品卡片，仅包含未删除且已上架（status=1）的商品。
     *
     * @param limit 查询上限，由业务层保证大于 0；名称对应 XML 中的绑定参数
     * @return 按销量降序、商品 ID 升序排列的商品卡片列表
     */
    List<ProductCardVO> selectTopSales(@Param("limit") Integer limit,
                                       @Param("categoryId") Long categoryId);

    List<ProductCardVO> selectProducts(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword);

    ProductDetailVO selectProductDetailById(@Param("id") String id);

    List<ProductDetailVO> selectProductDetailsByIds(
            @Param("ids") List<Long> ids);

    /** 批量查询商品摘要，保留下架和已删除商品供购物车判断状态。 */
    List<ProductSummaryDTO> selectByIds(@Param("ids") List<Long> ids);

}
