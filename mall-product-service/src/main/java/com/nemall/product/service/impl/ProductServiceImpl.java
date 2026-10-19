package com.nemall.product.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.api.dto.PurchaseItemsDTO;
import com.nemall.product.entity.ProductDetailData;
import com.nemall.product.exception.ProductBusinessException;
import com.nemall.product.mapper.ProductMapper;
import com.nemall.product.service.ProductService;
import com.nemall.product.vo.ProductCardVO;
import com.nemall.product.vo.ProductDetailVO;
import com.nemall.product.vo.ProductPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Comparator;
import java.util.List;

/**
 * 销量排序查询的实现，校验参数并记录数据库查询耗时和失败信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(PurchaseItemsDTO request) {

        List<PurchaseItemsDTO.Item> items = request.getItems().stream()
                .sorted(Comparator.comparing(
                        PurchaseItemsDTO.Item::getProductId
                ))
                .toList();

        for (PurchaseItemsDTO.Item item : items) {
            int affectedRows = productMapper.restoreStock(
                    item.getProductId(),
                    item.getQuantity()
            );

            if (affectedRows != 1) {
                throw new ProductBusinessException(
                        409, "商品库存回补失败"
                );
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProductSummaryDTO> deductStock(
            PurchaseItemsDTO request) {

        List<PurchaseItemsDTO.Item> items = request.getItems().stream()
                .sorted(Comparator.comparing(
                        PurchaseItemsDTO.Item::getProductId
                ))
                .toList();

        for (PurchaseItemsDTO.Item item : items) {
            int affectedRows = productMapper.deductStock(
                    item.getProductId(),
                    item.getQuantity()
            );

            if (affectedRows != 1) {
                throw new ProductBusinessException(
                        409, "商品不可购买、库存不足或购买数量不合法"
                );
            }
        }

        List<Long> ids = items.stream()
                .map(PurchaseItemsDTO.Item::getProductId)
                .toList();

        return productMapper.selectByIds(ids);
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductCardVO> listProducts(
            String sort, Integer limit, Long categoryId) {

        return productMapper.selectTopSales(limit, categoryId);
    }

    @Override
    public ProductPageVO listProductsByPage(
            Long categoryId, Integer pageNum, String keyword) {

        if (pageNum == null || pageNum < 1) {
            throw new IllegalArgumentException("pageNum 必须大于 0");
        }

        // 去掉首尾空格；未传关键词或只有空格时，不添加搜索条件
        String searchKeyword = keyword == null ? null : keyword.trim();
        if (searchKeyword != null && searchKeyword.isEmpty()) {
            searchKeyword = null;
        }

        try {
            // 当前页、每页 12 件，同时查询符合条件的总数
            PageHelper.startPage(pageNum, 12, true)
                    .setReasonable(false);

            List<ProductCardVO> products =
                    productMapper.selectProducts(categoryId, searchKeyword);

            PageInfo<ProductCardVO> pageInfo = new PageInfo<>(products);

            return new ProductPageVO(
                    pageInfo.getList(),
                    pageInfo.getTotal(),
                    pageNum
            );
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public List<ProductSummaryDTO> queryByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return productMapper.selectByIds(ids);
    }

    @Override
    public ProductDetailVO getProductDetail(String id) {
        ProductDetailData data =
                productMapper.selectProductDetailById(id);

        if (data == null) {
            throw new ProductBusinessException(404, "商品不存在");
        }

        List<String> images = data.getDetailImageUrls() == null
                ? List.of()
                : JSON.readValue(
                data.getDetailImageUrls(),
                new TypeReference<List<String>>() {}
        );

        return new ProductDetailVO(
                data.getId().toString(),
                data.getName(),
                data.getModel(),
                data.getCoverImageUrl(),
                data.getPrice(),
                data.getSalesCount(),
                data.getStock(),
                images
        );
    }
}
