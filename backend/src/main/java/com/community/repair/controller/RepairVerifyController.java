package com.community.repair.controller;

import com.community.repair.common.PageResult;
import com.community.repair.common.Result;
import com.community.repair.constant.VerifyConstants;
import com.community.repair.dto.AgentVerifyDTO;
import com.community.repair.dto.OrderVerifyDTO;
import com.community.repair.dto.PendingVerifyQueryDTO;
import com.community.repair.enums.RoleEnum;
import com.community.repair.security.RequireRole;
import com.community.repair.service.RepairVerifyService;
import com.community.repair.vo.AgentVerifyVO;
import com.community.repair.vo.OrderListVO;
import com.community.repair.vo.VerifyRecordVO;
import com.community.repair.vo.VerifyResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单验收管理模块 Controller（V-01 ~ V-04）。
 *
 * <p>对应需求 FR-5-01 ~ FR-5-07，接口文档 4.6.1 ~ 4.6.4 节。</p>
 *
 * <p><b>为什么再新建一个 Controller，而不是往既有 Controller 里加方法？</b>
 * 因为本模块继续遵循「<b>能新增就不修改</b>」的原则：模块二 {@code RepairOrderController}、
 * 模块三 {@code RepairDispatchController}、模块四 {@code RepairProgressController}
 * 同属前序模块的交付边界，本模块一字节都不动它们，
 * 从而保证前序模块的代码在整合工程中<b>逐字未变</b>。
 * 四个 Controller 的类级 {@code @RequestMapping} 同为 {@code /api/orders}，
 * Spring MVC 完全支持这种「同一基础路径、多 Controller 分担」的写法。</p>
 *
 * <p><b>路径冲突说明</b>：本模块的四个路径
 * （{@code /pending-verify}、{@code /{orderId}/verify}、{@code /{orderId}/verify-agent}、
 * {@code /{orderId}/verify-records}）与既有路径均不冲突——
 * {@code /pending-verify} 与模块三的 {@code /pending-dispatch}、{@code /grab-pool}
 * 同为「单段字面量」但取值各异；三个两段式路径的末段均为字面量，
 * 与模块二的 {@code /{orderId}/cancel}、{@code /{orderId}/resubmit}、
 * 模块三的 {@code /{orderId}/accept}、模块四的 {@code /{orderId}/start} / {@code /finish} /
 * {@code /records} / {@code /duration} 同样互不重叠，
 * Spring Boot 3 的 {@code PathPatternParser} 可精确分流。</p>
 *
 * <p><b>权限设计</b>（复用既有的 {@code JwtInterceptor} + {@code @RequireRole}，
 * <b>不新增任何鉴权组件、不修改拦截器一行代码</b>）：</p>
 * <ul>
 *   <li>类上<b>不加</b> {@code @RequireRole} —— 因为本 Controller 同时承载
 *       「居民 + 管理员」的 V-01、「仅居民」的 V-02、「仅管理员」的 V-03，
 *       以及「全部登录用户」的 V-04，统一改为<b>方法级</b>精确标注，
 *       利用拦截器「方法级优先于类级」的既有规则承载；</li>
 *   <li>V-01 / V-04 的<b>数据范围</b>（居民本人 / 维修人员本人受派 / 管理员全部）
 *       在 Service 层强制注入，前端无法通过任何入参绕过；</li>
 *   <li>全部写操作方法在 Service 层还有第二道「角色兜底校验」与「数据归属校验
 *       （仅工单提交者本人可验收）」，即使前端被绕过也无法越权（NFR-S-03）。</li>
 * </ul>
 *
 * <p><b>用户身份获取方式</b>：Controller 层不接收任何操作人 ID 入参，
 * 一律由 Service 层通过 {@link com.community.repair.security.UserContext} 读取
 * JWT 解析出的登录信息，与模块零 / 一 / 二 / 三 / 四完全一致。</p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class RepairVerifyController {

    private final RepairVerifyService repairVerifyService;

    // ==================================================================================
    // 一、V-01 待验收工单快捷查询 —— 居民、系统管理员
    // ==================================================================================

    /**
     * V-01 待验收工单快捷查询。
     *
     * <p>GET /api/orders/pending-verify　居民、系统管理员</p>
     *
     * <p>等价于 {@code GET /api/orders?status=4}，但额外支持「仅看超时未验收」
     * （{@code onlyOverdue}）与「按完工时间区间筛选」两项验收专属能力。
     * 响应元素与 O-02 列表完全一致，其中 {@code verifyOverdue} 为重点字段
     * （FR-5-06，阈值 {@code OrderConstants.VERIFY_OVERDUE_HOURS} = 48 小时）。
     * 居民仅返回本人提交的待验收工单；维修人员调用返回 403；无数据时 {@code list} 为空数组。</p>
     */
    @GetMapping("/pending-verify")
    @RequireRole({RoleEnum.RESIDENT, RoleEnum.ADMIN})
    public Result<PageResult<OrderListVO>> pagePendingVerify(PendingVerifyQueryDTO query) {
        return Result.success(VerifyConstants.MSG_QUERY_SUCCESS,
                repairVerifyService.pagePendingVerify(query));
    }

    // ==================================================================================
    // 二、V-02 验收通过 / 驳回 —— 仅居民（且须为该工单提交者）
    // ==================================================================================

    /**
     * V-02 验收通过 / 驳回。
     *
     * <p>PUT /api/orders/{orderId}/verify　仅居民</p>
     *
     * <p>{@code verifyResult = 1}（通过）时状态 <b>4 → 5（已验收）</b>，
     * 写入验收快照并解锁服务评价入口；{@code verifyResult = 0}（驳回）时状态
     * <b>4 → 3（维修中）</b>，原受派维修人员不变，工单重新流入模块四返修。
     * 无论通过或驳回，均向 {@code repair_verify_log} 写入一行独立记录（BR-5-05）。</p>
     *
     * <p>数据归属：<b>仅工单提交者本人</b>可操作，否则返回 403（BR-5-06）。</p>
     */
    @PutMapping("/{orderId}/verify")
    @RequireRole(RoleEnum.RESIDENT)
    public Result<VerifyResultVO> verifyOrder(@PathVariable("orderId") Long orderId,
                                              @Valid @RequestBody OrderVerifyDTO dto) {
        VerifyResultVO vo = repairVerifyService.verifyOrder(orderId, dto);
        // 通过 / 驳回共用同一接口，提示语按结果区分，前端可直接展示
        String message = vo != null && Boolean.TRUE.equals(vo.getCanEvaluate())
                ? VerifyConstants.MSG_VERIFY_PASS_SUCCESS
                : VerifyConstants.MSG_VERIFY_REJECT_SUCCESS;
        return Result.success(message, vo);
    }

    // ==================================================================================
    // 三、V-03 管理员代验收 —— 仅系统管理员
    // ==================================================================================

    /**
     * V-03 管理员代验收。
     *
     * <p>PUT /api/orders/{orderId}/verify-agent　仅系统管理员</p>
     *
     * <p>待验收超时（超过 {@code VerifyConstants.OVERDUE_HOURS} = 48 小时）
     * 且居民长期未响应时，管理员代为验收。依据 BR-5-07 必须填写代验收说明，
     * 落库时 {@code repair_verify_log.verify_type = 2} 以与居民验收（1）区分。
     * <b>该接口仅支持验收通过，不支持代驳回</b>；未达超时阈值返回 409。</p>
     */
    @PutMapping("/{orderId}/verify-agent")
    @RequireRole(RoleEnum.ADMIN)
    public Result<AgentVerifyVO> agentVerify(@PathVariable("orderId") Long orderId,
                                             @Valid @RequestBody AgentVerifyDTO dto) {
        return Result.success(VerifyConstants.MSG_AGENT_VERIFY_SUCCESS,
                repairVerifyService.agentVerify(orderId, dto));
    }

    // ==================================================================================
    // 四、V-04 验收记录查询 —— 全部登录用户（受数据范围约束）
    // ==================================================================================

    /**
     * V-04 验收记录查询。
     *
     * <p>GET /api/orders/{orderId}/verify-records　全部登录用户（受数据范围约束）</p>
     *
     * <p>直查 {@code repair_verify_log}，按 {@code verifyRound} <b>升序</b>
     * 返回该工单全部轮次的验收记录，体现 BR-5-05「多次驳回 → 重新维修 → 再次验收
     * 均独立留痕」；{@code verifyType} 区分居民验收（1）与管理员代验收（2）。
     * 居民仅可查看本人工单、维修人员仅可查看本人受派工单、管理员可查看全部，
     * 不符时返回 403；工单无验收记录时返回空数组而非 404。</p>
     *
     * <p>本方法<b>不加</b> {@code @RequireRole}：与模块二的 O-02 / O-03 一样，
     * 只要携带合法 JWT 即可访问，具体可见性由 Service 层的数据归属校验决定。</p>
     */
    @GetMapping("/{orderId}/verify-records")
    public Result<List<VerifyRecordVO>> listVerifyRecords(@PathVariable("orderId") Long orderId) {
        return Result.success(VerifyConstants.MSG_QUERY_SUCCESS,
                repairVerifyService.listVerifyRecords(orderId));
    }
}
