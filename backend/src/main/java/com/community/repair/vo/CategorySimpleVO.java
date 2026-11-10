package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 新增类目返回 VO（对应接口 C-04：POST /api/categories 的 data）。
 *
 * <p>字段严格对齐接口文档 4.2.4 节：只返回新建类目的 ID、名称与创建时间。</p>
 */
@Data
public class CategorySimpleVO implements Serializable {

    /** 新建类目 ID */
    private Long categoryId;

    /** 类目名称 */
    private String categoryName;

    /** 创建时间 */
    private LocalDateTime createTime;
}
