package com.community.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 新增类目请求参数（对应接口 C-04：POST /api/categories）。
 *
 * <p>依据：FR-1-02、BR-1-01 ~ BR-1-03。</p>
 *
 * <p>说明：parentId 为 0 或 null 时表示创建一级类目；非 0 时必须指向一个已存在的一级类目。</p>
 */
@Data
public class CategoryCreateDTO implements Serializable {

    /** 类目名称，必填，2~20 个字符，同一父级下唯一（BR-1-01、BR-1-02） */
    @NotBlank(message = "请填写类目名称")
    @Size(min = 2, max = 20, message = "类目名称长度须为 2~20 个字符")
    private String categoryName;

    /** 父级类目 ID，默认 0（一级类目） */
    private Long parentId;

    /** 类目图标路径，选填 */
    private String icon;

    /** 类目说明，选填，≤ 255 字 */
    @Size(max = 255, message = "类目说明最多 255 个字符")
    private String description;

    /** 排序号，值越小越靠前，默认 0 */
    private Integer sortOrder;

    /** 状态 0 停用 / 1 启用，默认 1 启用 */
    private Integer status;
}
