package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当前登录用户信息 VO（对应接口 U-04：GET /api/auth/me，同时作为 U-02 登录返回的 userInfo 结构）。
 *
 * <p>依据：FR-8-06。用于前端顶栏展示、权限初始化与「个人信息维护」表单回显。</p>
 *
 * <p><b>不包含 password 字段</b>（BR-8-22）。</p>
 */
@Data
public class UserInfoVO implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 登录账号（只读） */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色编码：resident / repair / admin */
    private String role;

    /** 角色中文名：居民 / 维修人员 / 系统管理员 */
    private String roleName;

    /** 性别：0 未知 / 1 男 / 2 女 */
    private Integer gender;

    /** 手机号（只读，不可自行修改） */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 楼栋房号（仅居民返回） */
    private String address;

    /** 头像路径 */
    private String avatar;

    /** 维修工种（仅维修人员返回） */
    private String workerType;

    /** 技能描述（仅维修人员返回） */
    private String skillDesc;

    /** 工作状态（仅维修人员返回）：0 休息 / 1 空闲 / 2 忙碌 */
    private Integer workStatus;

    /** 账号状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 最近登录时间，从未登录为 null */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
