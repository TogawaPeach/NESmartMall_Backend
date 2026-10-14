package com.nemall.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {

    private String account;

    private String password;
}