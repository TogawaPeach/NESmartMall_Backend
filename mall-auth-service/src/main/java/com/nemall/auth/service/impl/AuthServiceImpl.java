package com.nemall.auth.service.impl;

import com.nemall.auth.dto.LoginDTO;
import com.nemall.auth.dto.RegisterDTO;
import com.nemall.auth.entity.Account;
import com.nemall.auth.mapper.AccountMapper;
import com.nemall.auth.security.JwtUtil;
import com.nemall.auth.service.AuthService;
import com.nemall.auth.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

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

    @Override
    @Transactional
    public LoginVO register(RegisterDTO registerDTO) {

        // 1. 检查必填字段
        if (registerDTO == null
                || registerDTO.getAccount() == null
                || registerDTO.getNickname() == null
                || registerDTO.getPassword() == null
                || registerDTO.getRole() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "注册信息不完整"
            );
        }

        // 账号、昵称去除首尾空格，密码保持原样
        String accountName = registerDTO.getAccount().trim();
        String nickname = registerDTO.getNickname().trim();
        String password = registerDTO.getPassword();
        Integer role = registerDTO.getRole();

        // 2. 检查手机号、昵称、密码及角色
        if (!accountName.matches("^1[3-9]\\d{9}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "请输入有效的11位手机号"
            );
        }

        if (nickname.isEmpty() || nickname.length() > 64) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "昵称须为1至64个字符"
            );
        }

        if (password.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "密码不能为空或全为空白"
            );
        }

        // BCrypt 按字节限制密码长度，不能直接按字符数判断
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "密码的长度不能超过72字节"
            );
        }

        if (role != 0 && role != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "账户类型只能是用户或商家"
            );
        }

        // 3. 提前检查手机号是否已经注册
        if (accountMapper.selectByAccount(accountName) != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "该手机号已注册"
            );
        }

        // 4. 创建账户实体，原始密码经过 BCrypt 后再保存
        Account account = new Account();
        account.setAccount(accountName);
        account.setNickname(nickname);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(role);

        // 不设置 status，使用数据库默认值 1

        // 5. 插入账户，数据库唯一约束处理并发注册冲突
        try {
            int rows = accountMapper.insert(account);

            if (rows != 1) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "创建账户失败"
                );
            }
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "该手机号已注册"
            );
        }

        // Mapper 必须把数据库生成的主键回填到 account.id
        if (account.getId() == null || account.getId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "未取得账户ID"
            );
        }

        // 6. 使用新建账户的 ID 和角色签发 JWT
        JwtUtil.TokenResult token = jwtUtil.createToken(
                account.getId(),
                account.getRole()
        );

        // 7. 返回与登录接口相同的数据结构
        LoginVO.UserVO user = new LoginVO.UserVO(
                account.getId().toString(),
                account.getAccount(),
                account.getNickname(),
                account.getRole()
        );

        return new LoginVO(
                token.accessToken(),
                "Bearer",
                token.expiresIn(),
                user
        );
    }
}