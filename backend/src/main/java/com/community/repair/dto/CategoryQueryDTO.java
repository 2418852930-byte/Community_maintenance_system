package com.community.repair.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 类目分页查询条件（对应接口 C-01：GET /api/categories）。
 *
 * <p>依据：FR-1-01。支持按类目名称模糊查询、按状态筛选、按父级类目筛选，
 * 并提供 {@code onlyEnabled} 开关供居民端报修选择器使用（BR-1-05）。</p>
 *
 * <p>分页参数命名遵循接口文档 2.5 节约定：pageNum 从 1 开始，pageSize 最大 100。</p>
 */
@Data
public class CategoryQueryDTO implements Serializable {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（Service 中做上限保护） */
    private Integer pageSize = 10;

    /** 类目名称关键字，模糊匹配 */
    private String categoryName;

    /** 类目状态：0 停用 / 1 启用 */
    private Integer status;

    /** 父级类目 ID，用于查询某父类下的子类目 */
    private Long parentId;

    /**
     * 为 true 时仅返回启用状态的类目。
     * <p>居民端报修页与下游工单提交模块调用时传 true（BR-1-05）。</p>
     */
    private Boolean onlyEnabled;
}
