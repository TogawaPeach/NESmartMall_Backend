package com.nemall.product.mapper;

import com.nemall.product.vo.ProductCardVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {

    List<ProductCardVO> selectTopSales(@Param("limit") Integer limit);
}