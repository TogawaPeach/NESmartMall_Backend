package com.nemall.order.dto;

import lombok.Data;

@Data
public class CreateSeckillOrderDTO {

    private String productId;

    private Recipient recipient;

    @Data
    public static class Recipient {

        private String name;

        private String phone;

        private String address;
    }
}
