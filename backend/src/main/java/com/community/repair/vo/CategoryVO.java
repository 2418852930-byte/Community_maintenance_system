package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 类目列表元素 / 详情 VO
 * （对应接口 C-01「类目分页列表」的 list 元素、C-03「类目详情」、C-05「修改类目」的返回）。
 *
 * <p>字段严格对齐接口文档 4.2.1 节，额外提供 {@code parentName} 与 {@code statusName}
 * 两个派生字段，前端列表可直接展示，无需再翻译一次。</p>
 */
@Data
public class CategoryVO implements Serializable {

    /** 类目 ID */
    private Long categoryId;

    /** 类目名称 */
    private String categoryName;

    /** 父级类目 ID，0 表示一级类目 */
    private Long parentId;

    /** 父级类目名称，一级类目为 null */
    private String parentName;

    /** 类目图标路径 */
    private String icon;

    /** 类目说明 */
    private String description;

    /** 排序号，越小越靠前 */
    private Integer sortOrder;

    /** 状态：0 停用 / 1 启用 */
    private Integer status;

    /** 状态中文名：停用 / 启用 */
    private String statusName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
