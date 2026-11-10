package com.community.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.repair.entity.RepairEvaluation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务评价表 Mapper（模块六）。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 后即可直接使用
 * {@code insert / selectById / selectList / selectPage / selectCount / update} 等方法，
 * 查询条件全部用 {@code LambdaQueryWrapper} / {@code LambdaUpdateWrapper} 构造，
 * 与模块二 ~ 模块五的做法保持一致，<b>不引入 XML 映射文件</b>。</p>
 *
 * <p>逻辑删除由 {@code @TableLogic} 自动生效：所有查询都会自动追加 {@code deleted = 0}。</p>
 *
 * <p><b>本 Mapper 只暴露「新增 + 查询 + 一次性回复」能力</b>：
 * 依据 BR-6-05（评价提交后居民不可自行修改或删除，保证评价数据的客观性），
 * 本模块不提供任何修改评分 / 内容的接口，也不提供删除入口。</p>
 *
 * <h3>下游模块对接（FR-6-08「评价数据汇总」，由模块七 S-07 实现）</h3>
 * <p>本 Mapper 与 {@link RepairEvaluation} 实体是本模块向<b>报修统计分析模块</b>开放评价数据的
 * 唯一数据访问入口，模块七无需重复定义实体，直接注入本 Mapper 即可完成聚合统计，
 * 且已由建表脚本为本场景预置了索引：</p>
 * <ul>
 *   <li>{@code idx_worker_score(worker_id, total_score)} —— 支撑「按维修人员聚合平均分与评价数量」；</li>
 *   <li>{@code idx_create_time(create_time)} —— 支撑「按月份 / 时间区间聚合」。</li>
 * </ul>
 * <p>模块七既可直接调用 {@code selectList} / {@code selectPage} 取明细后聚合，
 * 也可复用本模块的 E-02 <b>评价分页列表</b>接口（已内置类目、维修人员、时间、评分筛选），
 * 与本模块「不额外增加需求之外的功能」的边界保持一致。
 * 例如「某维修人员的评价均分」可直接写为：</p>
 * <pre>
 * SELECT AVG(total_score) FROM repair_evaluation
 *  WHERE worker_id = ? AND deleted = 0
 * </pre>
 */
@Mapper
public interface RepairEvaluationMapper extends BaseMapper<RepairEvaluation> {
}
