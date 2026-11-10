package com.community.repair.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色分配与变更请求参数（对应接口 U-12：PUT /api/users/{userId}/role）。
 *
 * <p>依据：FR-8-11、BR-8-17、BR-8-18。</p>
 *
 * <p>业务约束（在 Service 层校验）：</p>
 * <ul>
 *   <li>不可变更本人账号的角色（BR-8-18）；</li>
 *   <li>变更后系统须至少保留一个状态为「启用」的管理员账号，否则返回 409；</li>
 *   <li>变更在下一次请求即生效，用户重新登录后前端菜单同步刷新（BR-8-17）。</li>
 * </ul>
 */
@Data
public class RoleUpdateDTO implements Serializable {

    /** 目标角色：resident / repair / admin */
    @NotBlank(message = "请选择目标角色")
    private String role;
}
