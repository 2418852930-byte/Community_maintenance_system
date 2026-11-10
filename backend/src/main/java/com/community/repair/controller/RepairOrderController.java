package com.community.repair.controller;

import com.community.repair.common.PageResult;
import com.community.repair.common.Result;
import com.community.repair.dto.OrderCancelDTO;
import com.community.repair.dto.OrderCreateDTO;
import com.community.repair.dto.OrderQueryDTO;
import com.community.repair.dto.OrderResubmitDTO;
import com.community.repair.dto.OrderUpdateDTO;
import com.community.repair.enums.RoleEnum;
import com.community.repair.security.RequireRole;
import com.community.repair.service.RepairOrderService;
import com.community.repair.vo.OrderCancelVO;
import com.community.repair.vo.OrderCreateVO;
import com.community.repair.vo.OrderDetailVO;
import com.community.repair.vo.OrderListVO;
import com.community.repair.vo.OrderResubmitVO;
import com.community.repair.vo.OrderUpdateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报修工单提交模块 Controller（O-01 ~ O-06）。
 *
 * <p>对应需求 FR-2-01 ~ FR-2-09，接口文档 4.3.1 ~ 4.3.6 节。</p>
 *
 * <p><b>权限设计</b>（复用已有 {@code JwtInterceptor} + {@code @RequireRole}，
 * <b>不新增任何鉴权组件、不修改拦截器一行代码</b>）：</p>
 * <ul>
 *   <li>类上<b>不加</b> {@code @RequireRole} → O-02 / O-03 只要携带合法 JWT 即可访问
 *       （接口文档标注为「全部登录用户」），具体能看到哪些数据由 Service 层按角色限定；</li>
 *   <li>O-01 / O-04 / O-06 方法上标注 {@code @RequireRole(RoleEnum.RESIDENT)} → <b>仅居民</b>；</li>
 *   <li>O-05 方法上标注 {@code @RequireRole({RoleEnum.RESIDENT, RoleEnum.ADMIN})}
 *       → 居民本人与管理员（接口文档 4.3.5 权限角色）；</li>
 *   <li>拦截器「方法级注解优先于类级注解」，因此两种粒度可以安全地混用。</li>
 * </ul>
 *
 * <p><b>用户身份获取方式</b>：Controller 层不接收任何 {@code residentId} 入参，
 * 一律由 Service 层通过 {@link com.community.repair.security.UserContext} 读取
 * JWT 解析出的登录信息（BR-2-06），与模块零、模块一的既有做法完全一致。</p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class RepairOrderController {

    private final RepairOrderService repairOrderService;

    // ==================================================================================
    // 一、写入类接口（O-01、O-04、O-05、O-06）
    // ==================================================================================

    /**
     * O-01 提交报修工单。
     *
     * <p>POST /api/orders　仅居民</p>
     *
     * <p>服务端自动生成工单编号、自动关联当前登录居民 ID 与所选类目 ID，
     * 并将状态固定为「待审核」（8）。提交后工单进入管理员审核工作台
     * （下游模块三的 {@code GET /api/orders?status=8}），审核通过后方进入待派单池。</p>
     */
    @PostMapping
    @RequireRole(RoleEnum.RESIDENT)
    public Result<OrderCreateVO> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success("提交成功，等待管理员审核", repairOrderService.createOrder(dto));
    }

    /**
     * O-04 修改工单。
     *
     * <p>PUT /api/orders/{orderId}　仅居民</p>
     *
     * <p>允许修改的状态为「待审核」（8）与「待派单」（0）；修改不改变工单状态、不触发重新审核。
     * 已驳回（9）请改用 O-06 重新提交接口。</p>
     */
    @PutMapping("/{orderId}")
    @RequireRole(RoleEnum.RESIDENT)
    public Result<OrderUpdateVO> updateOrder(@PathVariable("orderId") Long orderId,
                                             @Valid @RequestBody OrderUpdateDTO dto) {
        return Result.success("保存成功", repairOrderService.updateOrder(orderId, dto));
    }

    /**
     * O-05 撤销工单。
     *
     * <p>PUT /api/orders/{orderId}/cancel　居民本人、系统管理员</p>
     *
     * <p>允许撤销的状态严格为「待审核」（8）/「待派单」（0）/「已驳回」（9），
     * 其余状态一律返回 409（BR-2-07）。状态置为 7 并写入撤销原因。</p>
     */
    @PutMapping("/{orderId}/cancel")
    @RequireRole({RoleEnum.RESIDENT, RoleEnum.ADMIN})
    public Result<OrderCancelVO> cancelOrder(@PathVariable("orderId") Long orderId,
                                             @Valid @RequestBody OrderCancelDTO dto) {
        return Result.success("撤销成功", repairOrderService.cancelOrder(orderId, dto));
    }

    /**
     * O-06 驳回工单修改后重新提交（V2.0 新增）。
     *
     * <p>PUT /api/orders/{orderId}/resubmit　仅居民</p>
     *
     * <p>允许操作的状态<b>严格为「已驳回」（9）</b>；成功后状态回到「待审核」（8），
     * 重新进入管理员审核队列，同时清空驳回原因 / 驳回人 / 驳回时间三个快照字段（BR-3-10）。</p>
     */
    @PutMapping("/{orderId}/resubmit")
    @RequireRole(RoleEnum.RESIDENT)
    public Result<OrderResubmitVO> resubmitOrder(@PathVariable("orderId") Long orderId,
                                                 @Valid @RequestBody OrderResubmitDTO dto) {
        return Result.success("已重新提交，等待管理员审核", repairOrderService.resubmitOrder(orderId, dto));
    }

    // ==================================================================================
    // 二、查询类接口（O-02、O-03，全部登录用户，数据范围由 Service 按角色限定）
    // ==================================================================================

    /**
     * O-02 工单分页列表。
     *
     * <p>GET /api/orders　全部登录用户</p>
     *
     * <p><b>一个接口支撑多套视图</b>（按角色自动限定数据范围，需求文档 3.3）：</p>
     * <ul>
     *   <li>居民 → 「我的报修」：自动锁定 {@code resident_id = 本人}；</li>
     *   <li>维修人员 → 「我的工单」：自动锁定 {@code worker_id = 本人}；</li>
     *   <li>管理员 → 「待审核工单池」传 {@code status=8}、「待派单池」传 {@code status=0}、
     *       「全部工单总览」不传状态。</li>
     * </ul>
     * <p>状态支持多值（英文逗号分隔），列表不限制状态范围，10 态全流程均可见。</p>
     */
    @GetMapping
    public Result<PageResult<OrderListVO>> pageOrders(OrderQueryDTO query) {
        return Result.success("查询成功", repairOrderService.pageOrders(query));
    }

    /**
     * O-03 工单详情。
     *
     * <p>GET /api/orders/{orderId}　全部登录用户（受数据范围约束）</p>
     *
     * <p>返回工单完整信息，含类目、居民、维修人员、派单信息、审核驳回信息、
     * 验收信息，以及派生字段 {@code evaluated} 与 {@code canOperate}。
     * 居民仅可查看本人工单、维修人员仅可查看本人受派工单，否则返回 403（BR-2-06）。</p>
     */
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable("orderId") Long orderId) {
        return Result.success("查询成功", repairOrderService.getOrderDetail(orderId));
    }
}
