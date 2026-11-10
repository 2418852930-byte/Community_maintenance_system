package com.community.repair.constant;

/**
 * 报修类目模块业务常量。
 *
 * <p>与 {@link UserConstants} 保持同样的组织方式：把需求文档中出现的魔法数字、
 * 长度限制与统一提示语集中定义，便于论文说明与后续维护。</p>
 *
 * <p>对应需求：FR-1-01 ~ FR-1-06、BR-1-01 ~ BR-1-06。</p>
 */
public final class CategoryConstants {

    private CategoryConstants() {
    }

    // ------------------------- 层级与状态（repair_category） -------------------------

    /**
     * 一级类目的 parent_id 取值。
     * <p>BR-1-03：类目最多两级，{@code parent_id = 0} 表示一级类目。</p>
     */
    public static final long ROOT_PARENT_ID = 0L;

    /** 类目状态：停用 */
    public static final int STATUS_DISABLED = 0;

    /** 类目状态：启用 */
    public static final int STATUS_ENABLED = 1;

    // ------------------------- 字段长度限制 -------------------------

    /** 类目名称长度下限（BR-1-02：2~20 字符） */
    public static final int NAME_MIN_LENGTH = 2;

    /** 类目名称长度上限（BR-1-02：2~20 字符） */
    public static final int NAME_MAX_LENGTH = 20;

    /** 类目说明长度上限（对应字段 repair_category.description VARCHAR(255)） */
    public static final int DESCRIPTION_MAX_LENGTH = 255;

    /** 类目图标路径长度上限（对应字段 repair_category.icon VARCHAR(255)） */
    public static final int ICON_MAX_LENGTH = 255;

    // ------------------------- 分页 -------------------------

    /** 分页每页条数上限（接口文档 2.5 节：最大 100） */
    public static final int MAX_PAGE_SIZE = 100;

    /** 分页每页条数默认值 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    // ------------------------- 统一提示语（对齐接口文档 4.2 节状态码说明） -------------------------

    /** 类目不存在（404） */
    public static final String MSG_CATEGORY_NOT_FOUND = "类目不存在";

    /** 同一父类目下名称重复（BR-1-01，409） */
    public static final String MSG_NAME_DUPLICATE = "同一上级类目下已存在同名类目";

    /** 上级类目不存在（404） */
    public static final String MSG_PARENT_NOT_FOUND = "上级类目不存在";

    /** 层级超限：最多两级（BR-1-03，409） */
    public static final String MSG_LEVEL_EXCEED = "类目最多两级，不能在二级类目下继续新增子类目";

    /** 层级超限：带子类目的类目不能改为二级类目（BR-1-03，409） */
    public static final String MSG_HAS_CHILDREN_CANNOT_DEMOTE = "该类目下已存在子类目，不能再改为二级类目";

    /** 上级类目不能选择自身（400） */
    public static final String MSG_PARENT_CANNOT_BE_SELF = "上级类目不能选择类目自身";

    /** 已被工单引用，不允许删除（BR-1-04，409） */
    public static final String MSG_CATEGORY_REFERENCED = "该类目已被工单使用，无法删除，可改为停用";

    /** 已被工单引用，不允许变更层级（BR-1-04，409） */
    public static final String MSG_CATEGORY_REFERENCED_NO_MOVE = "该类目已被工单使用，仅允许修改名称与排序号，不能变更层级";

    /** 存在下级类目，须先删除（409） */
    public static final String MSG_HAS_CHILDREN = "请先删除其下级类目";

    /** 类目名称长度越界（BR-1-02，400） */
    public static final String MSG_NAME_LENGTH = "类目名称长度须为 2~20 个字符";

    /** 类目状态取值非法（400） */
    public static final String MSG_STATUS_INVALID = "类目状态取值只能是 0（停用）或 1（启用）";
}
