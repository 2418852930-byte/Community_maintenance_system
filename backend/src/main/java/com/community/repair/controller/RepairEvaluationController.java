package com.community.repair.controller;

import com.community.repair.common.PageResult;
import com.community.repair.common.Result;
import com.community.repair.constant.EvaluationConstants;
import com.community.repair.dto.EvaluationCreateDTO;
import com.community.repair.dto.EvaluationQueryDTO;
import com.community.repair.dto.EvaluationReplyDTO;
import com.community.repair.enums.RoleEnum;
import com.community.repair.security.RequireRole;
import com.community.repair.service.RepairEvaluationService;
import com.community.repair.vo.EvaluationCreateVO;
import com.community.repair.vo.EvaluationDetailVO;
import com.community.repair.vo.EvaluationReplyVO;
import com.community.repair.vo.EvaluationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务评价管理模块 Controller（E-01 ~ E-04）。
 *
 * <p>对应需求 FR-6-01 ~ FR-6-08，接口文档 4.7.1 ~ 4.7.4 节。</p>
 *
 * <p><b>为什么新建一个 Controller，而不是往既有 Controller 里加方法？</b>
 * 因为本模块继续遵循「<b>能新增就不修改</b>」的原则：模块零 ~ 模块五的全部 Controller
 * 都属前序模块的交付边界，本模块一字节都不动它们，
 * 从而保证前序模块的代码在整合工程中<b>逐字未变</b>。</p>
 *
 * <p><b>路径说明</b>：本模块使用全新的基础路径 {@code /api/evaluations}，
 * 与既有的 {@code /api/orders}、{@code /api/categories}、{@code /api/users}、
 * {@code /api/files} 等基础路径<b>完全不重叠</b>，不存在任何路由冲突；
 * 四个接口的路径形态也各不相同：
 * 单段集合（{@code /api/evaluations}，POST 提交 / GET 列表）、
 * 两段（{@code /api/evaluations/{evaluationId}}，GET 详情）、
 * 三段（{@code /api/evaluations/{evaluationId}/reply}，POST 回复），
 * Spring Boot 3 的 {@code PathPatternParser} 可精确分流。</p>
 *
 * <p><b>权限设计</b>（复用既有的 {@code JwtInterceptor} + {@code @RequireRole}，
 * <b>不新增任何鉴权组件、不修改拦截器一行代码</b>）：</p>
 * <ul>
 *   <li>类上<b>不加</b> {@code @RequireRole} —— 因为本 Controller 同时承载
 *       「仅居民」的 E-01、「全部登录用户」的 E-02/E-03、「仅管理员」的 E-04，
 *       统一改为<b>方法级</b>精确标注，利用拦截器「方法级优先于类级」的既有规则承载；</li>
 *   <li>E-02 / E-03 的<b>数据范围</b>（居民本人 / 维修人员本人受派 / 管理员全部）
 *       在 Service 层强制注入，前端无法通过任何入参绕过；</li>
 *   <li>全部写操作方法在 Service 层还有第二道「角色兜底校验」与「数据归属校验
 *       （仅工单报修人本人可评价）」，即使前端被绕过也无法越权（NFR-S-03）。</li>
 * </ul>
 *
 * <p><b>用户身份获取方式</b>：Controller 层不接收任何操作人 ID 入参，
 * 一律由 Service 层通过 {@link com.community.repair.security.UserContext} 读取
 * JWT 解析出的登录信息，与模块零 ~ 模块五完全一致。</p>
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class RepairEvaluationController {

    private final RepairEvaluationService repairEvaluationService;

    // ==================================================================================
    // 一、E-01 提交服务评价 —— 仅居民（且须为该工单报修人）
    // ==================================================================================

    /**
     * E-01 提交服务评价。
     *
     * <p>POST /api/evaluations　仅居民</p>
     *
     * <p>居民对<b>已验收(5)</b>的工单提交三项星级评分与文字评价；
     * 提交成功后工单状态由 <b>5 → 6（已完成）</b>，业务链路彻底闭环（BR-6-04）。
     * 同一工单只允许评价一次（BR-6-02），重复提交返回 409。</p>
     *
     * <p>数据归属：<b>仅工单报修人本人</b>可操作，否则返回 403（BR-6-06）。</p>
     */
    @PostMapping
    @RequireRole(RoleEnum.RESIDENT)
    public Result<EvaluationCreateVO> createEvaluation(@Valid @RequestBody EvaluationCreateDTO dto) {
        return Result.success(EvaluationConstants.MSG_CREATE_SUCCESS,
                repairEvaluationService.createEvaluation(dto));
    }

    // ==================================================================================
    // 二、E-02 评价分页列表 —— 全部登录用户（受数据范围约束）
    // ==================================================================================

    /**
     * E-02 评价分页列表。
     *
     * <p>GET /api/evaluations　全部登录用户（受数据范围约束）</p>
     *
     * <p>按角色自动限定数据范围：居民返回本人提交的评价（FR-6-04）；
     * 维修人员返回本人所接工单收到的评价（FR-6-07，匿名评价隐藏居民标识，BR-6-06）；
     * 管理员返回全部评价并支持按星级、时间、类目、维修人员筛选（FR-6-05）。</p>
     *
     * <p>本方法<b>不加</b> {@code @RequireRole}：与模块二的 O-02 / O-03 一样，
     * 只要携带合法 JWT 即可访问，具体可见性由 Service 层的数据范围注入决定。
     * 无数据时 {@code list} 为空数组而非 404。</p>
     */
    @GetMapping
    public Result<PageResult<EvaluationVO>> pageEvaluations(EvaluationQueryDTO query) {
        return Result.success(EvaluationConstants.MSG_QUERY_SUCCESS,
                repairEvaluationService.pageEvaluations(query));
    }

    // ==================================================================================
    // 三、E-03 评价详情 —— 全部登录用户（受数据范围约束）
    // ==================================================================================

    /**
     * E-03 评价详情。
     *
     * <p>GET /api/evaluations/{evaluationId}　全部登录用户（受数据范围约束）</p>
     *
     * <p>按 ID 查询单条评价的完整信息，并附加对应工单摘要
     * （{@code orderNo} / {@code title} / {@code categoryName} / {@code finishTime} / {@code totalCost}）。
     * 居民仅可查看本人评价、维修人员仅可查看本人相关评价、管理员可查看全部，
     * 不符返回 403，不存在返回 404。</p>
     */
    @GetMapping("/{evaluationId}")
    public Result<EvaluationDetailVO> getEvaluationDetail(@PathVariable("evaluationId") Long evaluationId) {
        return Result.success(EvaluationConstants.MSG_QUERY_SUCCESS,
                repairEvaluationService.getEvaluationDetail(evaluationId));
    }

    // ==================================================================================
    // 四、E-04 回复评价 —— 仅系统管理员
    // ==================================================================================

    /**
     * E-04 回复评价。
     *
     * <p>POST /api/evaluations/{evaluationId}/reply　仅系统管理员</p>
     *
     * <p>管理员对评价进行文字回复（5~200 字）。依据 BR-6-07，
     * <b>一条评价只能回复一次，回复后不可修改</b>，
     * 重复回复返回 409「该评价已回复，不可重复回复」。</p>
     */
    @PostMapping("/{evaluationId}/reply")
    @RequireRole(RoleEnum.ADMIN)
    public Result<EvaluationReplyVO> replyEvaluation(@PathVariable("evaluationId") Long evaluationId,
                                                     @Valid @RequestBody EvaluationReplyDTO dto) {
        return Result.success(EvaluationConstants.MSG_REPLY_SUCCESS,
                repairEvaluationService.replyEvaluation(evaluationId, dto));
    }
}
