package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 类目启用 / 停用返回 VO（对应接口 C-07：PUT /api/categories/{categoryId}/status 的 data）。
 *
 * <p>字段严格对齐接口文档 4.2.7 节。</p>
 */
@Data
public class CategoryStatusVO implements Serializable {

    /** 类目 ID */
    private Long categoryId;

    /** 变更后的状态：0 停用 / 1 启用 */
    private Integer status;

    /** 变更时间 */
    private LocalDateTime updateTime;
}
