package com.nemall.auth.controller;

import com.nemall.auth.dto.LoginDTO;
import com.nemall.auth.dto.RegisterDTO;
import com.nemall.auth.service.AuthService;
import com.nemall.auth.vo.LoginVO;
import com.nemall.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Result<LoginVO>> login(
            @RequestBody LoginDTO loginDTO) {

        // 检查账号和密码，不对密码进行 trim 等修改
        if (loginDTO.getAccount() == null
                || loginDTO.getAccount().isBlank()
                || loginDTO.getPassword() == null
                || loginDTO.getPassword().isBlank()) {

            return ResponseEntity.badRequest().body(
                    new Result<>(400, "账号和密码不能为空", null)
            );
        }

        // 交给业务层查询账户、验证密码和签发 JWT
        LoginVO loginVO = authService.login(loginDTO);

        return ResponseEntity.ok(
                new Result<>(200, "OK", loginVO)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<Result<LoginVO>> register(
            @RequestBody RegisterDTO registerDTO) {

        // 创建账户，并返回 JWT 和用户信息
        LoginVO loginVO = authService.register(registerDTO);

        return ResponseEntity.ok(
                new Result<>(200, "注册成功", loginVO)
        );
    }
}