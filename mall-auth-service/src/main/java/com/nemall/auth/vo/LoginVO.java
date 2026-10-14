package com.nemall.auth.vo;

public record LoginVO(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserVO user
) {

    public record UserVO(
            String id,
            String account,
            String nickname,
            Integer role
    ) {
    }
}