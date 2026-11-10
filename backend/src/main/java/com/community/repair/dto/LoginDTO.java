package com.community.repair.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 账号登录请求参数（对应接口 U-02：POST /api/auth/login）。
 *
 * <p>依据：FR-8-02、BR-8-07 ~ BR-8-10。支持「用户名 或 手机号 + 密码」登录。</p>
 */
@Data
public class LoginDTO implements Serializable {

    /** 登录账号：支持用户名或手机号，分别匹配 sys_user.username / sys_user.phone */
    @NotBlank(message = "请输入用户名或手机号")
    private String account;

    /** 登录密码明文（HTTPS 传输），6~20 位 */
    @NotBlank(message = "请输入密码")
    private String password;
}
