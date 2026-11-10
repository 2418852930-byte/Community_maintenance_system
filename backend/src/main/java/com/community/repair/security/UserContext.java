package com.community.repair.security;

import com.community.repair.common.BusinessException;
import com.community.repair.common.ResultCode;

/**
 * 当前登录用户上下文（基于 ThreadLocal）。
 *
 * <p>请求进入时由 {@link JwtInterceptor#preHandle} 写入，请求结束时由
 * {@link JwtInterceptor#afterCompletion} 清除，防止线程池复用导致的用户身份串号。</p>
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /** 写入当前登录用户 */
    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    /** 获取当前登录用户，未登录返回 null */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户，未登录直接抛 401。
     * <p>业务代码中需要用户身份时建议使用本方法。</p>
     */
    public static LoginUser require() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已失效");
        }
        return loginUser;
    }

    /** 获取当前登录用户 ID */
    public static Long getUserId() {
        return require().getUserId();
    }

    /** 获取当前登录用户角色编码 */
    public static String getRole() {
        return require().getRole();
    }

    /** 清除上下文（务必在请求结束时调用） */
    public static void clear() {
        HOLDER.remove();
    }
}
