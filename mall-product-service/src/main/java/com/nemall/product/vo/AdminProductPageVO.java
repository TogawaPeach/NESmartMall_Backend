package com.nemall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 商家端商品分页结果。
 */
@Data
@AllArgsConstructor
public class AdminProductPageVO {

    /** 当前页商品，无数据时为空列表。 */
    private List<AdminProductVO> list;

    /** 当前状态筛选条件下的商品总数。 */
    private long total;

    private int pageNum;

    private int pageSize;
}
