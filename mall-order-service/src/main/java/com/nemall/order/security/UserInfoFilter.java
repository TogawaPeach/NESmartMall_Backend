package com.nemall.order.security;

import com.nemall.common.security.CurrentUser;
import com.nemall.common.security.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 将网关传来的用户信息恢复到订单请求的上下文。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserInfoFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/api/orders") && !path.startsWith("/api/orders/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        UserContext.clear();

        try {
            Long userId = readUserId(request.getHeader("user-info"));
            String role = request.getHeader("user-role");

            UserContext.set(new CurrentUser(userId, role));
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private Long readUserId(String value) {
        Long userId;
        try {
            userId = Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("网关用户身份缺失或格式错误");
        }

        if (userId <= 0) {
            throw new IllegalStateException("网关用户身份必须为正整数");
        }
        return userId;
    }
}
