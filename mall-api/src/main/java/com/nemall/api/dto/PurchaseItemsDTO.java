package com.nemall.api.dto;

import lombok.Data;

import java.util.List;

/** 服务间传递的本次购买条目。 */
@Data
public class PurchaseItemsDTO {

    private List<Item> items;

    @Data
    public static class Item {

        private Long productId;

        private Integer quantity;
    }
}
