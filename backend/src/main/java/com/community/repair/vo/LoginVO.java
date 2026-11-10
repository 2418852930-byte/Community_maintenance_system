package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功返回 VO（对应接口 U-02：POST /api/auth/login）。
 *
 * <pre>
 * {
 *   "token": "eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy",
 *   "tokenType": "Bearer",
 *   "expiresIn": 7200,
 *   "userInfo": { ... }
 * }
 * </pre>
 *
 * <p>依据：FR-8-02、BR-8-07、BR-8-10 —— 前端按 userInfo.role 跳转对应首页。</p>
 */
@Data
public class LoginVO implements Serializable {

    /** JWT 令牌 */
    private String token;

    /** 令牌类型，固定 Bearer */
    private String tokenType = "Bearer";

    /** 令牌有效期（秒），固定 7200 */
    private Integer expiresIn;

    /** 登录用户信息（结构与 U-04 一致） */
    private UserInfoVO userInfo;
}
