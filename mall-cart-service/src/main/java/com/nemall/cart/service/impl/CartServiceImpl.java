package com.nemall.cart.service.impl;

import com.nemall.api.client.ProductClient;
import com.nemall.api.dto.ProductBatchQueryDTO;
import com.nemall.api.dto.ProductSummaryDTO;
import com.nemall.cart.dto.AddCartItemDTO;
import com.nemall.cart.entity.CartItem;
import com.nemall.cart.mapper.CartMapper;
import com.nemall.cart.service.CartService;
import com.nemall.cart.vo.CartItemVO;
import com.nemall.cart.vo.CartVO;
import com.nemall.common.response.Result;
import com.nemall.common.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final ProductClient productClient;

    @Override
    public CartVO getCart() {
        Long userId = UserContext.getUserId();

        List<CartItem> cartItems = cartMapper.selectByUserId(userId);

        if (cartItems.isEmpty()) {
            return new CartVO(List.of());
        }

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();

        ProductBatchQueryDTO request = new ProductBatchQueryDTO();
        request.setIds(productIds);

        Result<List<ProductSummaryDTO>> result =
                productClient.queryByIds(request);

        if (result == null
                || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null) {
            throw new IllegalStateException("商品信息查询失败");
        }

        Map<Long, ProductSummaryDTO> productMap = result.getData()
                .stream()
                .collect(Collectors.toMap(
                        ProductSummaryDTO::getId,
                        product -> product));

        List<CartItemVO> items = cartItems.stream()
                .map(cartItem -> buildItem(
                        cartItem,
                        productMap.get(cartItem.getProductId())))
                .toList();

        return new CartVO(items);
    }

    @Override
    public void addToCart(AddCartItemDTO request) {
        Long userId = UserContext.getUserId();
        Long productId = Long.valueOf(request.getProductId());
        Integer quantity = request.getQuantity();

        ProductBatchQueryDTO query = new ProductBatchQueryDTO();
        query.setIds(List.of(productId));

        Result<List<ProductSummaryDTO>> result =
                productClient.queryByIds(query);

        if (result == null
                || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null) {
            throw new IllegalStateException("商品信息查询失败");
        }

        ProductSummaryDTO product = result.getData().isEmpty()
                ? null : result.getData().getFirst();

        CartItem existing =
                cartMapper.selectByUserIdAndProductId(userId, productId);

        long totalQuantity = (existing == null ? 0L : existing.getQuantity())
                + quantity;

        if (product == null
                || product.getStatus() != 1
                || product.getIsDeleted() != 0
                || product.getPrice() == null
                || totalQuantity > product.getStock()
                || totalQuantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "商品不可购买、库存不足或加入数量超限"
            );
        }

        cartMapper.addOrIncrement(userId, productId, quantity);
    }

    private CartItemVO buildItem(
            CartItem cartItem,
            ProductSummaryDTO product) {

        CartItemVO item = new CartItemVO();
        item.setCartItemId(cartItem.getId().toString());
        item.setProductId(cartItem.getProductId().toString());
        item.setQuantity(cartItem.getQuantity());

        if (product == null) {
            item.setAvailability("DELETED");
            return item;
        }

        item.setName(product.getName());
        item.setModel(product.getModel());
        item.setCoverUrl(product.getCoverUrl());
        item.setPrice(product.getPrice());
        item.setStock(product.getStock());

        if (product.getIsDeleted() == 1) {
            item.setAvailability("DELETED");
        } else if (product.getStatus() != 1) {
            item.setAvailability("OFF_SHELF");
        } else if (Long.valueOf(0).equals(product.getStock())) {
            item.setAvailability("OUT_OF_STOCK");
        } else {
            item.setAvailability("AVAILABLE");
        }

        return item;
    }
}
