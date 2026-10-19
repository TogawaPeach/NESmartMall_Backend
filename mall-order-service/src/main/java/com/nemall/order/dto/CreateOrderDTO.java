package com.nemall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderDTO {

    @NotEmpty(message = "请选择购买商品")
    @Valid
    private List<@NotNull(message = "商品条目不能为空") Item> items;

    @NotNull(message = "请填写收货信息")
    @Valid
    private Recipient recipient;

    @Data
    public static class Item {

        @NotBlank(message = "商品ID不能为空")
        @Pattern(
                regexp = "^[1-9][0-9]*$",
                message = "商品ID必须为正整数"
        )
        private String productId;

        @NotNull(message = "购买数量不能为空")
        @Positive(message = "购买数量必须大于0")
        private Integer quantity;
    }

    @Data
    public static class Recipient {

        @NotBlank(message = "收货姓名不能为空")
        @Size(max = 100, message = "收货姓名不能超过100个字符")
        private String name;

        @NotBlank(message = "收货手机号不能为空")
        @Size(min = 7, max = 32, message = "收货手机号长度必须为7至32个字符")
        @Pattern(
                regexp = "^\\+?(?=(?:\\D*\\d){7})[0-9][0-9 ()-]{5,30}[0-9]$",
                message = "收货手机号格式不正确"
        )
        private String phone;

        @NotBlank(message = "收货地址不能为空")
        @Size(max = 500, message = "收货地址不能超过500个字符")
        private String address;
    }
}
