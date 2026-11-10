package com.community.repair.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户分页查询条件（对应接口 U-08：GET /api/users）。
 *
 * <p>依据：FR-8-07。支持关键字（用户名 / 真实姓名 / 手机号）模糊查询，
 * 并按角色、账号状态、工种、工作状态、创建时间区间组合筛选。</p>
 *
 * <p>分页参数命名遵循接口文档 2.5 节约定：pageNum 从 1 开始，pageSize 最大 100。</p>
 */
@Data
public class UserQueryDTO implements Serializable {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（Service 中做上限保护） */
    private Integer pageSize = 10;

    /** 角色筛选：resident / repair / admin */
    private String role;

    /** 关键字：模糊匹配 username / real_name / phone */
    private String keyword;

    /** 账号状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 维修工种（role = repair 时有效） */
    private String workerType;

    /** 工作状态（role = repair 时有效）：0 休息 / 1 空闲 / 2 忙碌 */
    private Integer workStatus;

    /** 创建时间起（yyyy-MM-dd HH:mm:ss） */
    private String startTime;

    /** 创建时间止（yyyy-MM-dd HH:mm:ss） */
    private String endTime;

    /** 排序字段（下划线命名），如 create_time、last_login_time */
    private String orderBy;

    /** 排序方向：asc / desc，默认 desc */
    private String orderDir = "desc";
}
