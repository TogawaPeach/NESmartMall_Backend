package com.nemall.gateway.security;

import com.nemall.common.response.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IdentityFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        boolean publicRequest = PublicPaths.isPublic(
                request.getMethod(),
                request.getRequestURI()
        );

        // 公开接口跳过校验；受保护接口校验失败则结束请求
        if (!publicRequest && !hasValidToken(request)) {
            unauthorized(response);
            return;
        }

        // 继续后续过滤链，由网关转发请求
        filterChain.doFilter(request, response);
    }

    private boolean hasValidToken(HttpServletRequest request) {
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // 缺少请求头或不是 Bearer 格式
        if (authorization == null
                || !authorization.regionMatches(
                true, 0,
                BEARER_PREFIX, 0,
                BEARER_PREFIX.length())) {
            return false;
        }

        String token =
                authorization.substring(BEARER_PREFIX.length());

        try {
            // 校验签名和时间声明；空 token 也会校验失败
            Jwt jwt = jwtDecoder.decode(token);

            // 要求 token 必须包含过期时间
            return jwt.getExpiresAt() != null;
        } catch (JwtException exception) {
            return false;
        }
    }

    private void unauthorized(HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

        objectMapper.writeValue(
                response.getWriter(),
                new Result<Void>(401, "请登录后再操作", null)
        );
    }
}