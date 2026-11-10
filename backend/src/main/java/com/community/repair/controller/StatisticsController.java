package com.community.repair.controller;

import com.community.repair.common.Result;
import com.community.repair.constant.StatisticsConstants;
import com.community.repair.dto.StatisticsQueryDTO;
import com.community.repair.enums.RoleEnum;
import com.community.repair.security.RequireRole;
import com.community.repair.service.StatisticsService;
import com.community.repair.vo.StatisticsCategoryVO;
import com.community.repair.vo.StatisticsOverviewVO;
import com.community.repair.vo.StatisticsStatusVO;
import com.community.repair.vo.StatisticsTrendVO;
import com.community.repair.vo.WorkerStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报修统计分析模块 Controller（S-01 ~ S-05）。
 *
 * <p>对应需求 FR-7-01 ~ FR-7-04、FR-7-10 与业务规则 BR-7-01 ~ BR-7-05、BR-7-07；
 * 接口文档 4.8.1 ~ 4.8.5 节。</p>
 *
 * <h3>一、为什么新建一个 Controller？</h3>
 * <p>继续遵循前七个模块贯彻的「<b>能新增就不修改</b>」原则：
 * 模块零 ~ 模块六的全部 Controller 都属前序模块的交付边界，
 * 本模块一字节都不动它们，从而保证前序模块代码在整合工程中<b>逐字未变</b>。
 * 本模块使用全新的基础路径 {@code /api/statistics}，与既有的
 * {@code /api/orders}、{@code /api/categories}、{@code /api/dispatch}、
 * {@code /api/progress}、{@code /api/verify}、{@code /api/evaluations}、
 * {@code /api/users}、{@code /api/files} <b>完全不重叠</b>，不存在任何路由冲突；
 * 五个接口的子路径（{@code /overview}、{@code /trend}、{@code /category}、
 * {@code /status}、{@code /worker}）均为单段且互不相同，可精确分流。</p>
 *
 * <h3>二、权限设计（复用既有鉴权链路，不新增任何组件）</h3>
 * <ul>
 *   <li>类级统一标注 {@code @RequireRole(RoleEnum.ADMIN)}——本模块五个接口全部
 *       <b>仅对系统管理员开放</b>（BR-7-07，接口文档 4.8 节「权限：仅管理员」），
 *       因此直接放在类上，比逐个方法重复标注更不易遗漏；</li>
 *   <li>拦截链路由既有的 {@code JwtInterceptor} 承载：先校验 JWT 合法性（401），
 *       再比对角色（403），本模块<b>不修改拦截器一行代码</b>；</li>
 *   <li>Service 层还有一道角色兜底断言（{@code requireAdmin}），
 *       保证即使 Service 被定时任务 / 单元测试等其它入口直接调用，也不会泄漏统计数据
 *       （NFR-S-03 越权防护）。</li>
 * </ul>
 *
 * <h3>三、入参处理</h3>
 * <p>五个接口的筛选参数是「同一组参数的不同子集」，统一由
 * {@link StatisticsQueryDTO} 承载并使用 GET 查询串传参，
 * 这样既符合统计查询「无副作用、可缓存、可收藏 URL」的语义，
 * 也让时间范围 / 类目 / 维修人员三类条件的<b>解析与校验逻辑在五个接口上完全一致</b>。
 * 时间格式同时兼容 {@code yyyy-MM-dd HH:mm:ss} 与 {@code yyyy-MM-dd}，
 * 各字段的默认值与取值域校验全部在 Service 层完成（越界统一返回 400）。</p>
 *
 * <p><b>关于参数校验的位置</b>：{@link StatisticsQueryDTO} 是 GET 查询串绑定的 POJO，
 * 其字段上的 {@code @Min/@Max} 只在参数标注 {@code @Valid} 时才会被 Spring 触发，
 * 而本模块所有取值域校验（granularity / level / topN / urgency / 时间格式 / 时间跨度）
 * <b>统一收敛在 Service 层</b>完成——这样做的目的是让「非法值 → 400 提示语」
 * 只有一处来源，逐字对齐接口文档 4.8 节的约定文案，
 * 不会出现「DTO 校验先命中返回笼统提示、Service 校验后命中返回精确提示」的两套文案分裂。
 * 因此本类<b>不标注</b> {@code @Validated}、参数也<b>不标注</b> {@code @Valid}。</p>
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    // ==================================================================================
    // 一、S-01 统计概览看板
    // ==================================================================================

    /**
     * S-01 统计概览看板（FR-7-10）。
     *
     * <p>GET /api/statistics/overview　仅系统管理员</p>
     *
     * <p>一次性返回累计有效工单数、今日新增、待审核 / 待派单 / 处理中 / 待验收 /
     * 已验收 / 已完成 / 已撤销 / 已驳回各项计数，以及维修完成率、平均综合评分、
     * 平均维修时长、平均接单时长、评价率，供前端渲染首页指标卡。</p>
     *
     * <p>默认时间范围：<b>当年 1 月 1 日 00:00:00 ～ 当前时间</b>。
     * 本接口不接受类目 / 维修人员筛选（接口文档 4.8.1 只定义时间范围）。</p>
     *
     * @param query 查询条件（仅取用 startTime / endTime）
     * @return 统一响应体，{@code data} 为概览指标对象
     */
    @GetMapping("/overview")
    @RequireRole(RoleEnum.ADMIN)
    public Result<StatisticsOverviewVO> getOverview(StatisticsQueryDTO query) {
        return Result.success(StatisticsConstants.MSG_QUERY_SUCCESS,
                statisticsService.getOverview(query));
    }

    // ==================================================================================
    // 二、S-02 报修量趋势统计
    // ==================================================================================

    /**
     * S-02 报修量趋势统计（FR-7-01）。
     *
     * <p>GET /api/statistics/trend　仅系统管理员</p>
     *
     * <p>按日 / 周 / 月统计工单提交量，输出连续时间序列供前端绘制<b>折线图</b>；
     * 序列元素含 {@code orderCount}、{@code completedCount}、{@code cancelCount}、
     * {@code rejectedCount} 四个计数。</p>
     *
     * <p>默认时间范围：<b>近 30 天 ～ 当前时间</b>；默认粒度 {@code day}。
     * 时间跨度超过 730 天返回 400（BR-7-05）；
     * 区间内没有任何工单时返回空数组与 {@code total = 0}。</p>
     *
     * @param query 查询条件（granularity / startTime / endTime / categoryId / workerId / urgency）
     * @return 统一响应体，{@code data} 为趋势对象
     */
    @GetMapping("/trend")
    @RequireRole(RoleEnum.ADMIN)
    public Result<StatisticsTrendVO> getTrend(StatisticsQueryDTO query) {
        return Result.success(StatisticsConstants.MSG_QUERY_SUCCESS,
                statisticsService.getTrend(query));
    }

    // ==================================================================================
    // 三、S-03 类目分布统计
    // ==================================================================================

    /**
     * S-03 类目分布统计（FR-7-02）。
     *
     * <p>GET /api/statistics/category　仅系统管理员</p>
     *
     * <p>按报修类目统计工单数量与占比，供前端绘制<b>饼图 / 环形图</b>。
     * 默认按一级类目聚合（{@code level = 1}），传 {@code level = 2} 切换为二级类目粒度；
     * {@code topN} 默认 10、上限 50。</p>
     *
     * <p>统计范围含待审核(8)，不含已撤销(7) 与已驳回(9)（对齐 BR-7-04）；
     * {@code total} 为独立查询得到的有效报修量，充当各项 {@code ratio} 的统一分母。</p>
     *
     * @param query 查询条件（startTime / endTime / level / topN / categoryId / workerId）
     * @return 统一响应体，{@code data} 为类目分布对象
     */
    @GetMapping("/category")
    @RequireRole(RoleEnum.ADMIN)
    public Result<StatisticsCategoryVO> getCategoryDistribution(StatisticsQueryDTO query) {
        return Result.success(StatisticsConstants.MSG_QUERY_SUCCESS,
                statisticsService.getCategoryDistribution(query));
    }

    // ==================================================================================
    // 四、S-04 工单状态分布统计
    // ==================================================================================

    /**
     * S-04 工单状态分布统计（FR-7-03）。
     *
     * <p>GET /api/statistics/status　仅系统管理员</p>
     *
     * <p>返回 <b>0 ~ 9 共 10 态完整分布</b>（无数据的状态项也会出现，其 {@code orderCount} 为 0），
     * 状态中文名统一取自模块二的 {@code OrderStatusEnum}，
     * 「已撤销(7)」与「已驳回(9)」单独成列、不计入 {@code total}。
     * 供前端渲染状态分布表 / 状态占比条。</p>
     *
     * @param query 查询条件（startTime / endTime / categoryId / workerId）
     * @return 统一响应体，{@code data} 为状态分布对象
     */
    @GetMapping("/status")
    @RequireRole(RoleEnum.ADMIN)
    public Result<StatisticsStatusVO> getStatusDistribution(StatisticsQueryDTO query) {
        return Result.success(StatisticsConstants.MSG_QUERY_SUCCESS,
                statisticsService.getStatusDistribution(query));
    }

    // ==================================================================================
    // 五、S-05 维修人员工作量统计
    // ==================================================================================

    /**
     * S-05 维修人员工作量统计（FR-7-04）。
     *
     * <p>GET /api/statistics/worker　仅系统管理员</p>
     *
     * <p>统计各维修人员的接单量、完成量、在途量、撤销量、平均维修时长与平均综合评分，
     * 默认按完成量降序排列（完成量相同再按接单量降序，保证排名稳定），
     * 供前端绘制<b>柱状图</b>与工作量排名表。</p>
     *
     * <p>口径：待审核(8) / 已驳回(9) / 已撤销(7) 未进入派单链路（{@code worker_id} 恒为空），
     * 天然不计入任何人员指标（对齐建表脚本附录 C2）；
     * 无数据时返回空数组而非 404。</p>
     *
     * @param query 查询条件（startTime / endTime / workerId / categoryId）
     * @return 统一响应体，{@code data} 为人员工作量排名列表
     */
    @GetMapping("/worker")
    @RequireRole(RoleEnum.ADMIN)
    public Result<List<WorkerStatVO>> getWorkerWorkload(StatisticsQueryDTO query) {
        return Result.success(StatisticsConstants.MSG_QUERY_SUCCESS,
                statisticsService.getWorkerWorkload(query));
    }
}
