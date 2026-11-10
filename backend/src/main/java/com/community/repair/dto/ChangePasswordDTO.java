package com.community.repair.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改本人密码请求参数（对应接口 U-06：PUT /api/auth/password）。
 *
 * <p>依据：FR-8-05、BR-8-13 —— 用户只能修改本人密码，且必须校验原密码是否正确。</p>
 */
@Data
public class ChangePasswordDTO implements Serializable {

    /** 原密码：必须与数据库中的密文匹配，否则返回 409「原密码错误」 */
    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    /** 新密码：6~20 位且须同时包含字母与数字（BR-8-03） */
    @NotBlank(message = "请输入新密码")
    private String newPassword;

    /** 确认新密码：必须与 newPassword 完全一致（BR-8-04） */
    @NotBlank(message = "请再次输入新密码")
    private String confirmPassword;
}
