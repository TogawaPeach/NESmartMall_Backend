package com.nemall.common.security;

/** 保存当前线程处理的请求身份，请求结束后需要清理。 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> CONTEXT =
            new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        CONTEXT.set(user);
    }

    public static CurrentUser get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        CurrentUser user = get();

        if (user == null) {
            throw new IllegalStateException("当前请求未登录");
        }

        return user.accountId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
