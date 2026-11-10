package com.community.repair.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 类目启用 / 停用请求参数（对应接口 C-07：PUT /api/categories/{categoryId}/status）。
 *
 * <p>依据：FR-1-05、BR-1-05。</p>
 */
@Data
public class CategoryStatusUpdateDTO implements Serializable {

    /** 目标状态：0 停用 / 1 启用 */
    @NotNull(message = "请选择目标状态")
    @Min(value = 0, message = "类目状态取值只能是 0 或 1")
    @Max(value = 1, message = "类目状态取值只能是 0 或 1")
    private Integer status;
}
