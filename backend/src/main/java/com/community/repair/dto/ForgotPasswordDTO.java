package com.community.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码 / 自助重置密码请求参数（对应接口 U-05：POST /api/auth/forgot-password）。
 *
 * <p>依据：FR-8-04、BR-8-11、BR-8-12。</p>
 *
 * <p>业务规则：须同时校验「用户名」与「注册预留手机号」，二者匹配方可重置；
 * 毕业设计范围内不实现短信验证码校验。重置成功后清零登录失败计数。</p>
 */
@Data
public class ForgotPasswordDTO implements Serializable {

    /** 用户名 */
    @NotBlank(message = "请输入用户名")
    private String username;

    /** 注册预留手机号 */
    @NotBlank(message = "请输入注册预留手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String phone;

    /** 新密码：6~20 位且须同时包含字母与数字（BR-8-03），且不得与原密码相同（BR-8-12） */
    @NotBlank(message = "请输入新密码")
    private String newPassword;

    /** 确认新密码：必须与 newPassword 完全一致（BR-8-04） */
    @NotBlank(message = "请再次输入新密码")
    private String confirmPassword;
}
