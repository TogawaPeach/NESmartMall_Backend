package com.nemall.gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class MutableHeaderRequest extends HttpServletRequestWrapper {

    // HTTP 请求头名称不区分大小写
    private final Map<String, List<String>> headers =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    MutableHeaderRequest(HttpServletRequest request) {
        super(request);

        // 复制原请求的全部请求头，包括同名头的多个值
        Enumeration<String> names = request.getHeaderNames();

        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(
                    name,
                    Collections.list(request.getHeaders(name))
            );
        }
    }

    // 覆盖该请求头，同时清除原有的多个值
    void setHeader(String name, String value) {
        headers.put(name, List.of(value));
    }

    // 删除该请求头及其全部值
    void removeHeader(String name) {
        headers.remove(name);
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name);

        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = headers.get(name);

        return Collections.enumeration(
                values == null ? List.of() : values
        );
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }
}