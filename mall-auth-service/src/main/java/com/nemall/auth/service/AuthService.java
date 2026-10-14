package com.nemall.auth.service;

import com.nemall.auth.dto.LoginDTO;
import com.nemall.auth.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO loginDTO);
}