package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色变更结果 VO（对应接口 U-12：PUT /api/users/{userId}/role 的返回 data）。
 *
 * <p>依据：FR-8-11、BR-8-17。</p>
 */
@Data
public class RoleChangeVO implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 变更前角色编码 */
    private String oldRole;

    /** 变更前角色中文名 */
    private String oldRoleName;

    /** 变更后角色编码 */
    private String role;

    /** 变更后角色中文名 */
    private String roleName;

    /** 变更时间 */
    private LocalDateTime updateTime;

    /**
     * 提示语：角色变更在下一次请求即生效，用户重新登录后前端菜单同步刷新（BR-8-17）。
     */
    private String tip;
}
