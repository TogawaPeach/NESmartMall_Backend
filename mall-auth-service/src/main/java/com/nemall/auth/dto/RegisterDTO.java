package com.nemall.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO {

    // 手机号，作为登录账号
    private String account;

    // 用户昵称；商家注册时作为店铺名
    private String nickname;

    // 前端提交的原始密码
    private String password;

    // 0：商家，1：普通用户
    private Integer role;
}