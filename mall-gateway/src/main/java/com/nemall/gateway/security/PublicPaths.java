package com.nemall.gateway.security;

import java.util.Set;

public final class PublicPaths {

    // 公开接口：请求方法 + 精确路径
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "POST /api/auth/login",
            "GET /api/products",
            "POST /api/auth/register"
    );

    private PublicPaths() {
    }

    public static boolean isPublic(String method, String path) {
        return PUBLIC_PATHS.contains(method + " " + path);
    }
}