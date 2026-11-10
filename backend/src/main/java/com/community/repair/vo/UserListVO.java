package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户列表元素 VO（对应接口 U-08：GET /api/users 的 list 元素）。
 *
 * <p>依据：FR-8-07。字段严格对齐接口文档 4.1.8 节，
 * 额外提供 roleName / statusName 两个中文名字段，方便前端直接展示。</p>
 *
 * <p><b>不包含任何密码相关字段</b>（BR-8-22）。</p>
 */
@Data
public class UserListVO implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色编码：resident / repair / admin */
    private String role;

    /** 角色中文名 */
    private String roleName;

    /** 性别：0 未知 / 1 男 / 2 女 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 楼栋房号 */
    private String address;

    /** 维修工种 */
    private String workerType;

    /** 技能描述 */
    private String skillDesc;

    /** 工作状态：0 休息 / 1 空闲 / 2 忙碌 */
    private Integer workStatus;

    /** 账号状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 账号状态中文名：禁用 / 启用 */
    private String statusName;

    /** 最后登录时间，从未登录为 null */
    private LocalDateTime lastLoginTime;

    /** 是否处于登录失败锁定期（lock_until 晚于当前时间） */
    private Boolean locked;

    /** 创建时间 */
    private LocalDateTime createTime;
}
