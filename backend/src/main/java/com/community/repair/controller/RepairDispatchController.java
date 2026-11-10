package com.community.repair.controller;

import com.community.repair.common.PageResult;
import com.community.repair.common.Result;
import com.community.repair.constant.DispatchConstants;
import com.community.repair.dto.BatchDispatchDTO;
import com.community.repair.dto.GrabPoolQueryDTO;
import com.community.repair.dto.GrabPublishDTO;
import com.community.repair.dto.OrderAcceptDTO;
import com.community.repair.dto.OrderAuditDTO;
import com.community.repair.dto.OrderDispatchDTO;
import com.community.repair.dto.OrderReassignDTO;
import com.community.repair.dto.OrderRejectDTO;
import com.community.repair.dto.PendingDispatchQueryDTO;
import com.community.repair.enums.AuditResultEnum;
import com.community.repair.enums.RoleEnum;
import com.community.repair.security.RequireRole;
import com.community.repair.service.RepairDispatchService;
import com.community.repair.vo.BatchDispatchVO;
import com.community.repair.vo.DispatchRecordVO;
import com.community.repair.vo.GrabPoolItemVO;
import com.community.repair.vo.GrabPublishVO;
import com.community.repair.vo.OrderAcceptVO;
import com.community.repair.vo.OrderAuditVO;
import com.community.repair.vo.OrderDispatchVO;
import com.community.repair.vo.OrderGrabVO;
import com.community.repair.vo.OrderListVO;
import com.community.repair.vo.OrderReassignVO;
import com.community.repair.vo.OrderRejectVO;
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
 * 工单派单调度模块 Controller（D-02 ~ D-12）。
 *
 * <p>对应需求 FR-3-01 ~ FR-3-12，接口文档 4.4.2 ~ 4.4.12 节。</p>
 *
 * <p><b>为什么新建一个 Controller，而不是往模块二的 {@code RepairOrderController} 里加方法？</b>
 * 因为本模块遵循「能新增就不修改」的原则：模块二的 Controller 属于其交付边界，
 * 本模块一字节都不动它，从而保证前序模块的代码在整合工程中<b>逐字未变</b>。
 * 两个 Controller 的类级 {@code @RequestMapping} 同为 {@code /api/orders}，
 * Spring MVC 完全支持这种「同一基础路径、多 Controller 分担」的写法。</p>
 *
 * <p><b>路径优先级说明</b>：本模块存在 {@code GET /api/orders/grab-pool}、
 * {@code GET /api/orders/pending-dispatch}、{@code PUT /api/orders/batch-dispatch}
 * 等<b>字面量路径</b>，与模块二的 {@code GET/PUT /api/orders/{orderId}}
 * 形似冲突。Spring Boot 3 的 {@code PathPatternParser} 对「字面量段」的匹配优先级
 * <b>高于</b>「变量段」，因此 {@code grab-pool} 等请求一定命中本模块的语义化接口，
 * 不会误入模块二的 {@code {orderId}} 映射（若误入则会因类型转换失败而报错，可据此察觉）。</p>
 *
 * <p><b>权限设计</b>（复用既有的 {@code JwtInterceptor} + {@code @RequireRole}，
 * <b>不新增任何鉴权组件、不修改拦截器一行代码</b>）：</p>
 * <ul>
 *   <li>类上<b>不加</b> {@code @RequireRole} —— 因为本 Controller 同时承载管理员侧
 *       （审核 / 派单 / 改派 / 发布抢单池 / 待派单池）与维修人员侧
 *       （抢单池 / 抢单 / 接单 / 拒单）两类接口，统一改为<b>方法级</b>精确标注，
 *       利用拦截器「方法级优先」的既有规则，避免出现越权放行；</li>
 *   <li>全部写操作方法在 Service 层还有第二道「角色兜底校验」与「数据归属校验」，
 *       即使前端被绕过也无法越权（NFR-S-03）。</li>
 * </ul>
 *
 * <p><b>用户身份获取方式</b>：Controller 层不接收任何操作人 ID 入参，
 * 一律由 Service 层通过 {@link com.community.repair.security.UserContext} 读取
 * JWT 解析出的登录信息，与模块零 / 一 / 二完全一致。</p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class RepairDispatchController {

    private final RepairDispatchService repairDispatchService;

    // ==================================================================================
    // 一、审核环节（D-12）—— 8 → 0（通过）/ 8 → 9（驳回）
    // ==================================================================================

    /**
     * D-12 工单审核（通过 / 驳回）。
     *
     * <p>PUT /api/orders/{orderId}/audit　仅系统管理员</p>
     *
     * <p>允许审核的状态严格为 8（待审核）。审核通过 → 状态 0 并进入待派单池；
     * 审核驳回 → 状态 9 且必须填写驳回原因（5~200 字），
     * 居民端「我的报修」会立即看到驳回原因并可走 O-06 重新提交。</p>
     */
    @PutMapping("/{orderId}/audit")
    @RequireRole(RoleEnum.ADMIN)
    public Result<OrderAuditVO> auditOrder(@PathVariable("orderId") Long orderId,
                                           @Valid @RequestBody OrderAuditDTO dto) {
        OrderAuditVO vo = repairDispatchService.auditOrder(orderId, dto);
        // 提示语与接口文档 4.4.12 的响应示例逐字对齐（通过 / 驳回各一套）
        String message = AuditResultEnum.PASS.getCode().equals(dto.getAuditResult())
                ? DispatchConstants.MSG_AUDIT_PASS_SUCCESS
                : DispatchConstants.MSG_AUDIT_REJECT_SUCCESS;
        return Result.success(message, vo);
    }

    // ==================================================================================
    // 二、管理员派单工作台（D-02 ~ D-05、D-11）
    // ==================================================================================

    /**
     * D-11 待派单池快捷查询。
     *
     * <p>GET /api/orders/pending-dispatch　仅系统管理员</p>
     *
     * <p>等价于 {@code GET /api/orders?status=0}，默认紧急工单置顶（BR-3-07），
     * 是管理员「审核（8）→ 派单（0）」两级工作台中的第二级入口。</p>
     */
    @GetMapping("/pending-dispatch")
    @RequireRole(RoleEnum.ADMIN)
    public Result<PageResult<OrderListVO>> pagePendingDispatch(PendingDispatchQueryDTO query) {
        return Result.success(DispatchConstants.MSG_QUERY_SUCCESS,
                repairDispatchService.pagePendingDispatch(query));
    }

    /**
     * D-02 派单给维修人员。
     *
     * <p>PUT /api/orders/{orderId}/dispatch　仅系统管理员</p>
     *
     * <p>仅状态 0（待派单）可派单；目标人员须为启用的维修人员。
     * 成功后写入受派人 / 派单人 / 派单时间 / 派单备注，状态置为 1（已派单），
     * 并留下一条 {@code dispatch_type = 1} 的审计记录。</p>
     */
    @PutMapping("/{orderId}/dispatch")
    @RequireRole(RoleEnum.ADMIN)
    public Result<OrderDispatchVO> dispatchOrder(@PathVariable("orderId") Long orderId,
                                                 @Valid @RequestBody OrderDispatchDTO dto) {
        return Result.success(DispatchConstants.MSG_DISPATCH_SUCCESS,
                repairDispatchService.dispatchOrder(orderId, dto));
    }

    /**
     * D-03 批量派单。
     *
     * <p>PUT /api/orders/batch-dispatch　仅系统管理员</p>
     *
     * <p>勾选 1~50 条待派单工单一次性指派给同一维修人员；
     * 状态不合法的工单进入返回的失败明细，不阻断其余工单。</p>
     */
    @PutMapping("/batch-dispatch")
    @RequireRole(RoleEnum.ADMIN)
    public Result<BatchDispatchVO> batchDispatch(@Valid @RequestBody BatchDispatchDTO dto) {
        return Result.success(DispatchConstants.MSG_BATCH_DISPATCH_DONE,
                repairDispatchService.batchDispatch(dto));
    }

    /**
     * D-04 改派。
     *
     * <p>PUT /api/orders/{orderId}/reassign　仅系统管理员</p>
     *
     * <p>仅「状态 1 已派单且维修人员尚未接单」的工单可改派；
     * 状态保持 1，仅更换受派人，并留下成对记录原 / 新受派人的改派审计。</p>
     */
    @PutMapping("/{orderId}/reassign")
    @RequireRole(RoleEnum.ADMIN)
    public Result<OrderReassignVO> reassignOrder(@PathVariable("orderId") Long orderId,
                                                 @Valid @RequestBody OrderReassignDTO dto) {
        return Result.success(DispatchConstants.MSG_REASSIGN_SUCCESS,
                repairDispatchService.reassignOrder(orderId, dto));
    }

    /**
     * D-05 发布抢单池。
     *
     * <p>PUT /api/orders/{orderId}/publish-grab　仅系统管理员</p>
     *
     * <p>仅状态 0 可发布；发布后状态仍为 0，但工单进入抢单池可被 D-06 检索到。
     * 重复发布返回 409。</p>
     */
    @PutMapping("/{orderId}/publish-grab")
    @RequireRole(RoleEnum.ADMIN)
    public Result<GrabPublishVO> publishGrabPool(@PathVariable("orderId") Long orderId,
                                                 @Valid @RequestBody GrabPublishDTO dto) {
        return Result.success(DispatchConstants.MSG_PUBLISH_SUCCESS,
                repairDispatchService.publishGrabPool(orderId, dto));
    }

    // ==================================================================================
    // 三、维修人员侧（D-06 ~ D-09）
    // ==================================================================================

    /**
     * D-06 抢单池列表。
     *
     * <p>GET /api/orders/grab-pool　仅维修人员</p>
     *
     * <p>返回「已发布至抢单池且尚未被领取」的工单，紧急工单优先展示（BR-3-07）。</p>
     */
    @GetMapping("/grab-pool")
    @RequireRole(RoleEnum.REPAIR)
    public Result<PageResult<GrabPoolItemVO>> pageGrabPool(GrabPoolQueryDTO query) {
        return Result.success(DispatchConstants.MSG_QUERY_SUCCESS,
                repairDispatchService.pageGrabPool(query));
    }

    /**
     * D-07 抢单。
     *
     * <p>PUT /api/orders/{orderId}/grab　仅维修人员</p>
     *
     * <p>先到先得锁定工单：状态 0 → 1 并写入 {@code worker_id}；
     * 并发冲突时返回 409「该工单已被其他维修人员领取」（BR-3-04）。</p>
     */
    @PutMapping("/{orderId}/grab")
    @RequireRole(RoleEnum.REPAIR)
    public Result<OrderGrabVO> grabOrder(@PathVariable("orderId") Long orderId) {
        return Result.success(DispatchConstants.MSG_GRAB_SUCCESS,
                repairDispatchService.grabOrder(orderId));
    }

    /**
     * D-08 接单。
     *
     * <p>PUT /api/orders/{orderId}/accept　仅维修人员（且须为该工单受派人）</p>
     *
     * <p>状态 1 → 2，记录接单时间，并生成进度时间线的首个节点。</p>
     */
    @PutMapping("/{orderId}/accept")
    @RequireRole(RoleEnum.REPAIR)
    public Result<OrderAcceptVO> acceptOrder(@PathVariable("orderId") Long orderId,
                                             @Valid @RequestBody(required = false) OrderAcceptDTO dto) {
        // 接单备注是可选字段，允许完全不传 body（例如前端不弹窗直接点「接单」）
        OrderAcceptDTO safeDto = dto == null ? new OrderAcceptDTO() : dto;
        return Result.success(DispatchConstants.MSG_ACCEPT_SUCCESS,
                repairDispatchService.acceptOrder(orderId, safeDto));
    }

    /**
     * D-09 拒单。
     *
     * <p>PUT /api/orders/{orderId}/reject　仅维修人员（且须为该工单受派人）</p>
     *
     * <p>状态 1 → 0 回退并清空受派人信息，必须填写 5~200 字的拒单原因（BR-3-05）。</p>
     */
    @PutMapping("/{orderId}/reject")
    @RequireRole(RoleEnum.REPAIR)
    public Result<OrderRejectVO> rejectOrder(@PathVariable("orderId") Long orderId,
                                             @Valid @RequestBody OrderRejectDTO dto) {
        return Result.success(DispatchConstants.MSG_REJECT_ORDER_SUCCESS,
                repairDispatchService.rejectOrder(orderId, dto));
    }

    // ==================================================================================
    // 四、派单记录查询（D-10）—— 管理员与受派维修人员
    // ==================================================================================

    /**
     * D-10 派单记录查询。
     *
     * <p>GET /api/orders/{orderId}/dispatch-records　系统管理员、受派维修人员</p>
     *
     * <p>按 {@code dispatchTime} 升序返回该工单的完整派单轨迹（含派单人、被派人、
     * 时间、结果四要素）。维修人员查询非本人受派工单时返回 403，
     * 该数据范围校验在 Service 层完成。</p>
     */
    @GetMapping("/{orderId}/dispatch-records")
    @RequireRole({RoleEnum.ADMIN, RoleEnum.REPAIR})
    public Result<List<DispatchRecordVO>> listDispatchRecords(@PathVariable("orderId") Long orderId) {
        return Result.success(DispatchConstants.MSG_QUERY_SUCCESS,
                repairDispatchService.listDispatchRecords(orderId));
    }
}
