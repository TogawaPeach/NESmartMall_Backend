package com.nemall.auth.service.impl;

import com.nemall.auth.dto.LoginDTO;
import com.nemall.auth.entity.Account;
import com.nemall.auth.mapper.AccountMapper;
import com.nemall.auth.security.JwtUtil;
import com.nemall.auth.service.AuthService;
import com.nemall.auth.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountMapper accountMapper;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginDTO loginDTO) {

        // 第一步：根据前端传来的账号，查询账户
        Account account =
                accountMapper.selectByAccount(loginDTO.getAccount());

        // 第二步：账户不存在，登录失败
        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "账号或密码错误"
            );
        }

        // 第三步：验证密码
        boolean passwordCorrect = passwordEncoder.matches(
                loginDTO.getPassword(),
                account.getPasswordHash()
        );

        if (!passwordCorrect) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "账号或密码错误"
            );
        }

        // 第四步：检查账户状态，1 才能登录
        if (!Integer.valueOf(1).equals(account.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "账户已禁用"
            );
        }

        // 第五步：使用数据库中的 ID 和角色生成 JWT
        JwtUtil.TokenResult token = jwtUtil.createToken(
                account.getId(),
                account.getRole()
        );

        // 第六步：整理返回给前端的用户信息
        LoginVO.UserVO user = new LoginVO.UserVO(
                account.getId().toString(),
                account.getAccount(),
                account.getNickname(),
                account.getRole()
        );

        // 第七步：返回令牌和用户信息
        return new LoginVO(
                token.accessToken(),
                "Bearer",
                token.expiresIn(),
                user
        );
    }
}