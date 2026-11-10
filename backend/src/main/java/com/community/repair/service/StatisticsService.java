package com.community.repair.service;

import com.community.repair.dto.StatisticsQueryDTO;
import com.community.repair.vo.StatisticsCategoryVO;
import com.community.repair.vo.StatisticsOverviewVO;
import com.community.repair.vo.StatisticsStatusVO;
import com.community.repair.vo.StatisticsTrendVO;
import com.community.repair.vo.WorkerStatVO;

import java.util.List;

/**
 * 报修统计分析模块 服务接口（模块七）。
 *
 * <p>对应需求 FR-7-01 ~ FR-7-04、FR-7-08、FR-7-10 与业务规则 BR-7-01 ~ BR-7-05、BR-7-07；
 * 接口文档 4.8.1 ~ 4.8.5 节 S-01 ~ S-05。</p>
 *
 * <p><b>模块定位</b>：本模块是纯「数据汇总模块」，位于业务链路的最下游——
 * 上游承接模块二（工单提交）、模块三（派单）、模块四（维修进度）、模块五（验收）、
 * 模块六（评价）产生的全部业务数据，输出多维度统计结果。
 * 因此本模块<b>不写任何业务数据</b>，五个方法全部是只读查询，
 * 也不存在事务边界（无需 {@code @Transactional}）。</p>
 *
 * <p><b>权限</b>：五个方法全部仅对系统管理员开放（BR-7-07）。
 * 校验分两层：Controller 上的 {@code @RequireRole(RoleEnum.ADMIN)} 是权威拦截，
 * Service 内的角色兜底断言保证即使被其它入口直接调用也依然安全。</p>
 *
 * <p><b>时间参数默认值（各接口不同，已在方法注释中逐一标明）</b>：
 * S-01 默认「当年 1 月 1 日 ～ 现在」、S-02 默认「近 30 天 ～ 现在」、
 * S-03 ~ S-05 默认「当年 1 月 1 日 ～ 现在」；
 * 所有接口的时间跨度上限均为 730 天（BR-7-05）。</p>
 */
public interface StatisticsService {

    /**
     * S-01 统计概览看板（接口文档 4.8.1，需求 FR-7-10）。
     *
     * <p>一次性返回累计有效工单数、今日新增、待审核 / 待派单 / 处理中 / 待验收 / 已完成 /
     * 已撤销 / 已驳回各项计数，以及维修完成率、平均综合评分、平均维修时长、平均接单时长、评价率。</p>
     *
     * <p>默认时间范围：<b>当年 1 月 1 日 00:00:00 ～ 当前时间</b>（接口文档 4.8.1）。
     * 本接口不接受类目 / 维修人员筛选。</p>
     *
     * @param query 查询条件（仅取用 startTime / endTime）
     * @return 概览指标对象；无数据时数值类为 0、评分为 {@code null}
     */
    StatisticsOverviewVO getOverview(StatisticsQueryDTO query);

    /**
     * S-02 报修量趋势统计（接口文档 4.8.2，需求 FR-7-01）。
     *
     * <p>按日 / 周 / 月统计工单提交数量，输出时间序列供前端绘制折线图。
     * 序列元素含 {@code orderCount}（该时间点提交总量）、{@code completedCount}、
     * {@code cancelCount}、{@code rejectedCount} 四个计数。</p>
     *
     * <p>默认时间范围：<b>近 30 天 ～ 当前时间</b>（接口文档 4.8.2）；
     * 默认粒度 {@code day}。区间内没有任何工单时返回空数组与 {@code total = 0}。</p>
     *
     * @param query 查询条件（granularity / startTime / endTime / categoryId / workerId / urgency）
     * @return 趋势对象（含回显的实际粒度、区间总量、时间序列）
     */
    StatisticsTrendVO getTrend(StatisticsQueryDTO query);

    /**
     * S-03 类目分布统计（接口文档 4.8.3，需求 FR-7-02）。
     *
     * <p>按报修类目统计工单数量与占比，输出饼图 / 环形图数据。
     * 默认按一级类目聚合，可通过 {@code level = 2} 切换为二级类目粒度。</p>
     *
     * <p>统计范围含待审核(8)，不含已撤销(7) 与已驳回(9)（对齐 BR-7-04）。
     * {@code total} 为独立查询得到的<b>有效报修量</b>，充当各项 {@code ratio} 的统一分母；
     * 因此当 {@code topN} 截断长尾类目时，各项占比之和会小于 100%，属预期行为。</p>
     *
     * @param query 查询条件（startTime / endTime / level / topN / categoryId / workerId）
     * @return 类目分布对象（total + 按工单量降序的类目列表）
     */
    StatisticsCategoryVO getCategoryDistribution(StatisticsQueryDTO query);

    /**
     * S-04 工单状态分布统计（接口文档 4.8.4，需求 FR-7-03）。
     *
     * <p>返回 <b>0 ~ 9 共 10 态的完整分布</b>（无数据的状态项也会出现，其 {@code orderCount} 为 0），
     * 状态中文名统一取自模块二的 {@code OrderStatusEnum}。
     * 「已撤销(7)」与「已驳回(9)」单独成列，不计入 {@code total}（有效报修量）。</p>
     *
     * @param query 查询条件（startTime / endTime / categoryId / workerId）
     * @return 状态分布对象（total = 有效报修量 + 10 态列表，按状态码升序）
     */
    StatisticsStatusVO getStatusDistribution(StatisticsQueryDTO query);

    /**
     * S-05 维修人员工作量统计（接口文档 4.8.5，需求 FR-7-04）。
     *
     * <p>统计各维修人员的接单量、完成量、在途量、撤销量、平均维修时长与平均综合评分，
     * 默认按完成量降序排列，供绩效评估与派单均衡参考。</p>
     *
     * <p>口径：接单量 = 累计受派工单数；完成量 = 已验收(5) + 已完成(6)；
     * 在途量 = 已派单(1) + 已接单(2) + 维修中(3) + 待验收(4)。
     * 待审核(8) / 已驳回(9) 未进入派单链路，不计入任何指标（对齐建表脚本附录 C2）。</p>
     *
     * @param query 查询条件（startTime / endTime / workerId / categoryId）
     * @return 人员工作量排名列表；无数据时为空数组
     */
    List<WorkerStatVO> getWorkerWorkload(StatisticsQueryDTO query);
}
