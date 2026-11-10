package com.community.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.repair.entity.RepairCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 报修类目表 Mapper。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 后即可直接使用：
 * selectById / selectList / selectPage / insert / updateById / update 等常用方法，
 * 本模块的条件查询（名称模糊、状态、父级）全部使用 LambdaQueryWrapper 构造。</p>
 *
 * <p>逻辑删除由 {@code @TableLogic} 自动生效：所有查询都会自动追加 {@code deleted = 0}。</p>
 */
@Mapper
public interface RepairCategoryMapper extends BaseMapper<RepairCategory> {

    /**
     * 统计某个类目被多少条<b>未删除</b>的报修工单引用。
     *
     * <p>用途：实现 BR-1-04「已被工单引用的类目不允许删除，仅允许停用」，
     * 以及「已被引用的类目不允许变更层级」。</p>
     *
     * <p>说明：这里刻意只写一条统计 SQL，而<b>不新建 repair_order 的实体与 Mapper</b>——
     * 报修工单实体属于模块二（报修工单提交模块）的职责范围。等模块二实现后，
     * 可把本方法替换为 {@code repairOrderMapper.selectCount(...)}，调用方无需改动。</p>
     *
     * @param categoryId 类目ID
     * @return 引用该工单的未删除工单数量
     */
    @Select("SELECT COUNT(*) FROM repair_order WHERE category_id = #{categoryId} AND deleted = 0")
    long countOrdersByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 统计某个类目下<b>未删除</b>的直接子类目数量。
     *
     * <p>用途：实现接口文档 4.2.6 的「存在下级类目不允许删除，须先删除下级类目」校验。</p>
     *
     * @param parentId 父级类目ID
     * @return 子类目数量
     */
    @Select("SELECT COUNT(*) FROM repair_category WHERE parent_id = #{parentId} AND deleted = 0")
    long countChildrenByParentId(@Param("parentId") Long parentId);
}
