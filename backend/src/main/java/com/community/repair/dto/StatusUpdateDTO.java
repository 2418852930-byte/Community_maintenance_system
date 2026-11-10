package com.community.repair.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 账号启用 / 禁用请求参数（对应接口 U-13：PUT /api/users/{userId}/status）。
 *
 * <p>依据：FR-8-12、BR-8-09、BR-8-18、BR-8-19。</p>
 *
 * <p>业务约束（在 Service 层校验）：</p>
 * <ul>
 *   <li>管理员不得禁用本人账号 → 403；</li>
 *   <li>禁用系统中最后一个启用状态的管理员 → 409（BR-8-18）；</li>
 *   <li>禁用后该账号无法登录，其既有令牌在下次请求即被拒绝（BR-8-09）。</li>
 * </ul>
 */
@Data
public class StatusUpdateDTO implements Serializable {

    /** 目标状态：0 禁用 / 1 启用 */
    @NotNull(message = "请选择目标状态")
    @Min(value = 0, message = "状态取值只能是 0 或 1")
    @Max(value = 1, message = "状态取值只能是 0 或 1")
    private Integer status;
}
