package com.nemall.cart.security;

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

/** 将网关传来的用户信息放入当前请求的上下文。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserInfoFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/api/cart") && !path.startsWith("/api/cart/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        UserContext.clear();

        try {
            Long userId = Long.valueOf(request.getHeader("user-info"));
            String role = request.getHeader("user-role");

            UserContext.set(new CurrentUser(userId, role));
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理，避免线程复用时残留上一个用户的信息。
            UserContext.clear();
        }
    }
}
