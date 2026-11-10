package com.community.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.repair.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 报修工单表 Mapper。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 后即可直接使用：
 * selectById / selectList / selectPage / selectCount / insert / updateById / update 等常用方法。
 * 本模块的列表查询条件（状态多值、类目、紧急程度、关键字、时间区间）全部使用
 * {@code LambdaQueryWrapper} 构造，与模块一的写法保持一致，<b>不引入 XML 映射文件</b>。</p>
 *
 * <p>逻辑删除由 {@code @TableLogic} 自动生效：所有查询都会自动追加 {@code deleted = 0}。</p>
 *
 * <p><b>与模块一的衔接</b>：模块一的 {@code RepairCategoryMapper.countOrdersByCategoryId}
 * 曾以一条统计 SQL 代为实现「类目是否被工单引用」（BR-1-04），并在注释中预留了
 * 「模块二实现后可替换为 {@code repairOrderMapper.selectCount(...)}」的说明。
 * 本模块实现后，该替换已具备条件，但为保证<b>不破坏模块一既有功能</b>，
 * 本次不改动模块一代码，仅在 README 中记录该优化点。</p>
 */
@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    /**
     * 查询指定编号前缀下<b>最大</b>的工单编号，用于生成当日流水号（BR-2-05）。
     *
     * <p>前缀形如 {@code BX20260918}，返回形如 {@code BX202609180007}；
     * 若当日尚无工单则返回 {@code null}，由 Service 从 1 开始编号。</p>
     *
     * <p>实现说明：{@code order_no} 上已有唯一索引 {@code uk_order_no}，
     * 本查询可直接走该索引的「有序扫描 + 取首行」，效率为 O(1)；
     * 同时由唯一索引为并发提交兜底（Service 层捕获冲突后重试）。</p>
     *
     * @param orderNoPrefix 编号前缀，如 BX20260918
     * @return 当日最大工单编号，无记录时返回 null
     */
    @Select("SELECT order_no FROM repair_order "
            + "WHERE order_no LIKE CONCAT(#{orderNoPrefix}, '%') AND deleted = 0 "
            + "ORDER BY order_no DESC LIMIT 1")
    String selectMaxOrderNoByPrefix(@Param("orderNoPrefix") String orderNoPrefix);

    /**
     * 统计某工单是否已存在<b>未删除</b>的服务评价。
     *
     * <p>用途：工单详情（O-03）的派生字段 {@code evaluated}——前端据此控制
     * 「去评价」入口的显隐（一单一评，BR-6-02）。</p>
     *
     * <p>说明：与模块一 {@code RepairCategoryMapper.countOrdersByCategoryId} 采用同样的
     * 「轻量统计 SQL」策略——{@code repair_evaluation} 的实体与 Mapper 属于
     * <b>模块六（服务评价管理模块）</b>的职责范围，本模块按「不额外增加需求之外的功能」
     * 的要求不越界创建，仅用一条 {@code @Select} 完成只读统计。
     * 待模块六实现后，可替换为 {@code repairEvaluationMapper.selectCount(...)}，调用方无需改动。</p>
     *
     * @param orderId 工单ID
     * @return 该工单的未删除评价条数（0 或 1）
     */
    @Select("SELECT COUNT(*) FROM repair_evaluation WHERE order_id = #{orderId} AND deleted = 0")
    long countEvaluationByOrderId(@Param("orderId") Long orderId);
}
