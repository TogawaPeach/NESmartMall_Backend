package com.nemall.cart.mapper;

import com.nemall.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {

    List<CartItem> selectByUserId(
            @Param("userId") Long userId);

    CartItem selectByUserIdAndProductId(
            @Param("userId") Long userId,
            @Param("productId") Long productId);

    int addOrIncrement(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity);

    int updateQuantity(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity);

    int deleteByUserIdAndProductId(
            @Param("userId") Long userId,
            @Param("productId") Long productId);
}
