package com.community.repair.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报修类目表实体（对应《02-建表SQL（方案A）》第 2 张表 repair_category）。
 *
 * <p>承载模块：<b>模块一 报修类目管理模块</b>（FR-1-01 ~ FR-1-06）。
 * 本表是系统的「基础数据字典」，为下游报修工单提交模块提供分类数据支撑：
 * {@code repair_order.category_id} 直接关联本表主键。</p>
 *
 * <p>关键约定：</p>
 * <ul>
 *   <li>层级：{@code parent_id = 0} 为一级类目，非 0 时指向一级类目的 category_id，
 *       <b>最多两级</b>（BR-1-03）；</li>
 *   <li>status：0 停用 / 1 启用，停用类目不出现在居民端报修选择器中（BR-1-05）；</li>
 *   <li>sort_order：值越小越靠前，相同时按 create_time 倒序（BR-1-06）；</li>
 *   <li>deleted：逻辑删除标记，本表被 repair_order 引用，只允许逻辑删除。</li>
 * </ul>
 */
@Data
@TableName("repair_category")
public class RepairCategory implements Serializable {

    /** 类目ID，主键，自增 */
    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    /** 类目名称，2~20 个字符，同一父级下唯一（BR-1-01、BR-1-02） */
    private String categoryName;

    /** 父级类目ID，0 表示一级类目（BR-1-03：最多两级） */
    private Long parentId;

    /** 类目图标图片路径 */
    private String icon;

    /** 类目说明 / 适用范围 */
    private String description;

    /** 显示排序号，值越小越靠前（BR-1-06） */
    private Integer sortOrder;

    /** 类目状态：0-停用，1-启用（BR-1-05、FR-1-05） */
    private Integer status;

    /** 创建时间，由 MyMetaObjectHandler 插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，由 MyMetaObjectHandler 插入/更新时自动填充（建表方案 A 要求业务代码维护） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除。
     * <p>@JsonIgnore 保证该内部标记不会出现在任何响应中。</p>
     */
    @TableLogic
    @TableField("deleted")
    @JsonIgnore
    private Integer deleted;
}
