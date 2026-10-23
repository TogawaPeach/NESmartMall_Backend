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
import java.util.List;

/**
 * 在网关转发前清除外部身份头，为受保护请求恢复已验证的账户身份。
 */
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

        MutableHeaderRequest forwardedRequest =
                new MutableHeaderRequest(request);

        // 清除外部传入的身份头
        forwardedRequest.removeHeader("user-info");
        forwardedRequest.removeHeader("user-role");
        forwardedRequest.removeHeader("X-Service-Token");

        boolean publicRequest = PublicPaths.isPublic(
                request.getMethod(),
                request.getRequestURI()
        );

        // 公开接口继续匿名访问
        if (publicRequest) {
            filterChain.doFilter(forwardedRequest, response);
            return;
        }

        Jwt jwt = readJwt(request);

        if (jwt == null) {
            unauthorized(response);
            return;
        }

        String role = jwt.getClaimAsStringList("roles").getFirst();

        boolean productStatsRequest =
                "GET".equals(request.getMethod())
                && "/api/merchant/products/stats".equals(request.getRequestURI());

        if (productStatsRequest && !"MERCHANT".equals(role)) {
            forbidden(response);
            return;
        }

        // 从已验证的 JWT 提取身份，覆盖写入内部请求头
        forwardedRequest.setHeader("user-info", jwt.getSubject());
        forwardedRequest.setHeader("user-role", role);

        filterChain.doFilter(forwardedRequest, response);
    }

    /** 校验成功返回 JWT，缺失或不合法时返回 null，由调用处统一响应 401。 */
    private Jwt readJwt(HttpServletRequest request) {
        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null
                || !authorization.regionMatches(
                        true, 0,
                        BEARER_PREFIX, 0,
                        BEARER_PREFIX.length())) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length());

        try {
            Jwt jwt = jwtDecoder.decode(token);

            String userId = jwt.getSubject();

            // 账户 ID 必须是 Long 范围内的正整数
            if (jwt.getExpiresAt() == null
                    || userId == null
                    || !userId.matches("[1-9][0-9]*")
                    || Long.parseLong(userId) <= 0) {
                return null;
            }

            // roles 必须是数组，且仅包含一个已知角色
            Object rolesClaim = jwt.getClaims().get("roles");
            if (!(rolesClaim instanceof List<?> roles) || roles.size() != 1) {
                return null;
            }

            Object role = roles.getFirst();
            if (!"CUSTOMER".equals(role) && !"MERCHANT".equals(role)) {
                return null;
            }

            return jwt;
        } catch (JwtException | IllegalArgumentException exception) {
            return null;
        }
    }

    private void forbidden(HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getWriter(),
                new Result<Void>(403, "仅商家可访问", null)
        );
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
