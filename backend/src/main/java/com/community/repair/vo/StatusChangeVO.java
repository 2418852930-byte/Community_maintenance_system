package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 账号状态变更结果 VO（对应接口 U-13：PUT /api/users/{userId}/status 的返回 data）。
 *
 * <p>依据：FR-8-12、BR-8-09。</p>
 */
@Data
public class StatusChangeVO implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 变更后状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 变更后状态中文名 */
    private String statusName;

    /** 变更时间 */
    private LocalDateTime updateTime;

    /**
     * 提示语：禁用维修人员后，其在途工单需由管理员在派单模块执行改派（BR-8-19）。
     * 非维修人员或启用操作时为 null。
     */
    private String tip;
}
