package com.community.repair.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户详情 VO（对应接口 U-09：GET /api/users/{userId}）。
 *
 * <p>依据：FR-8-08。在 {@link UserListVO} 基础上追加登录安全状态字段，
 * 用于用户详情抽屉与账号解锁判断。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDetailVO extends UserListVO {

    /** 连续登录失败次数（BR-8-08） */
    private Integer loginFailCount;

    /** 账号锁定截止时间，未锁定为 null */
    private LocalDateTime lockUntil;

    /** 备注 */
    private String remark;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
