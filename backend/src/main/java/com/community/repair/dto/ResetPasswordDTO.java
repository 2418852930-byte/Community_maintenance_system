package com.community.repair.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 重置用户密码请求参数（对应接口 U-14：PUT /api/users/{userId}/reset-password）。
 *
 * <p>依据：FR-8-12 配套、BR-8-21。</p>
 *
 * <p>业务规则：管理员重置他人密码时，新密码须满足密码强度规则（BR-8-03），
 * 重置成功后清零该账号的登录失败次数与锁定状态；被重置用户下次登录后应及时修改密码。</p>
 */
@Data
public class ResetPasswordDTO implements Serializable {

    /** 新密码：6~20 位且须同时包含字母与数字（BR-8-03） */
    @NotBlank(message = "请输入新密码")
    private String newPassword;

    /** 确认新密码：必须与 newPassword 完全一致（BR-8-04） */
    @NotBlank(message = "请再次输入新密码")
    private String confirmPassword;
}
