package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户简要信息 VO。
 *
 * <p>用于 U-01 居民账号注册、U-10 新增用户 的返回结构
 * （只返回新建账号的关键字段，避免暴露多余信息）。</p>
 */
@Data
public class UserSimpleVO implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色编码 */
    private String role;

    /** 角色中文名 */
    private String roleName;

    /** 手机号 */
    private String phone;

    /** 账号状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
