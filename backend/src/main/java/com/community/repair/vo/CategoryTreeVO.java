package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 类目树节点 VO（对应接口 C-02：GET /api/categories/tree）。
 *
 * <p>依据：FR-1-06、BR-1-03（类目最多两级）。</p>
 *
 * <p>用途：</p>
 * <ul>
 *   <li>管理员类目管理页展示两级结构；</li>
 *   <li><b>下游报修工单提交模块</b>的级联选择器（Cascader）数据源，
 *       调用时传 {@code onlyEnabled = true} 即可拿到「启用的一级类目 + 其启用的子类目」（BR-1-05）。</li>
 * </ul>
 */
@Data
public class CategoryTreeVO implements Serializable {

    /** 类目 ID */
    private Long categoryId;

    /** 类目名称 */
    private String categoryName;

    /** 父级类目 ID，一级类目为 0 */
    private Long parentId;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态：0 停用 / 1 启用 */
    private Integer status;

    /**
     * 子类目数组，结构同本层；无子类目时为空数组（接口文档约定，不返回 null）。
     * <p>初始化即赋空列表，避免序列化出 null 导致前端 Cascader 报错。</p>
     */
    private List<CategoryTreeVO> children = new ArrayList<>();
}
