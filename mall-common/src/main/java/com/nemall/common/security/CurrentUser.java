package com.nemall.common.security;

/** 当前请求的账户身份。 */
public record CurrentUser(Long accountId, String role) {
}
