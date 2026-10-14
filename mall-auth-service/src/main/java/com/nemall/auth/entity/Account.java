package com.nemall.auth.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Account {

    // 账户 ID，商家账号也用它作为商家 ID
    private Long id;

    // 登录账号
    private String account;

    // 昵称；商家账号这里存店铺名
    private String nickname;

    // 数据库保存的密码哈希
    private String passwordHash;

    // 0：商家，1：普通用户
    private Integer role;

    // 0：禁用，1：正常
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;
}