package com.community.repair.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改类目请求参数（对应接口 C-05：PUT /api/categories/{categoryId}）。
 *
 * <p>依据：FR-1-03、BR-1-01 ~ BR-1-04。</p>
 *
 * <p><b>局部更新语义</b>：所有字段均为选填，只提交需要修改的字段；
 * MyBatis-Plus 默认字段策略为 NOT_NULL，未传的字段不会进入 UPDATE 语句。</p>
 *
 * <p>长度与取值校验放在 Service 层完成，原因：本 DTO 的字段允许为 null（表示不修改），
 * 若在字段上直接加 {@code @Size}，前端传空字符串会误触发 400，语义不准确。</p>
 */
@Data
public class CategoryUpdateDTO implements Serializable {

    /** 类目名称，选填，若提交则须为 2~20 个字符且同父级下唯一 */
    private String categoryName;

    /** 父级类目 ID，选填；变更层级时受 BR-1-04 限制（已被工单引用则不允许变更） */
    private Long parentId;

    /** 类目图标路径 */
    private String icon;

    /** 类目说明，≤ 255 字 */
    private String description;

    /** 排序号，值越小越靠前 */
    private Integer sortOrder;

    /** 状态 0 停用 / 1 启用 */
    private Integer status;
}
