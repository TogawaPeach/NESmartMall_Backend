package com.nemall.product.service;

import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.api.dto.PurchaseItemsDTO;
import com.nemall.product.vo.AdminProductPageVO;
import com.nemall.product.vo.ProductCardVO;
import com.nemall.product.vo.ProductDetailVO;
import com.nemall.product.vo.ProductPageVO;
import com.nemall.product.vo.ProductStatsVO;

import java.util.List;

/**
 * 商品列表查询业务，负责校验查询条件并提供展示数据。
 */
public interface ProductService {

    /** 查询商品数量统计。 */
    ProductStatsVO getProductStats();

    /** 按商品状态分页查询商家端商品。 */
    AdminProductPageVO listMerchantProductsByPage(
            Integer pageNum, Integer pageSize, String status);

    /** 批量回补订单已扣库存，任一商品失败则整批回滚。 */
    void restoreStock(PurchaseItemsDTO request);

    /** 批量条件扣库存，任一商品失败则整批回滚。 */
    List<ProductSummaryDTO> deductStock(PurchaseItemsDTO request);

    /**
     * 查询已上架且未删除的商品。
     *
     * @param sort 排序方式，目前只支持 sales_desc
     * @param limit 返回数量，范围为 1～8
     * @param categoryId 分类 ID，null 表示全部，否则范围为 1～5
     * @return 按销量降序、同销量按商品 ID 升序排列的商品列表
     */
    List<ProductCardVO> listProducts(
            String sort, Integer limit, Long categoryId);

    ProductPageVO listProductsByPage(
            Long categoryId, Integer pageNum, String keyword);

    /**
     * 查询商品详情入口。
     *
     * @param id 商品 ID，使用字符串接收，后续校验合法性
     * @return 商品 ID 与对应的站内详情路径
     */
    ProductDetailVO getProductDetail(String id);

    /** 根据商品 ID 批量查询商品摘要。 */
    List<ProductSummaryDTO> queryByIds(List<Long> ids);
}
