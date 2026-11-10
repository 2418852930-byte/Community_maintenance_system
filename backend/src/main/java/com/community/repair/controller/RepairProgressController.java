package com.community.repair.controller;

import com.community.repair.common.Result;
import com.community.repair.constant.ProgressConstants;
import com.community.repair.dto.OrderFinishDTO;
import com.community.repair.dto.OrderRecordCreateDTO;
import com.community.repair.dto.OrderStartDTO;
import com.community.repair.enums.RoleEnum;
import com.community.repair.security.RequireRole;
import com.community.repair.service.RepairProgressService;
import com.community.repair.vo.OrderDurationVO;
import com.community.repair.vo.OrderFinishVO;
import com.community.repair.vo.OrderRecordCreateVO;
import com.community.repair.vo.OrderRecordVO;
import com.community.repair.vo.OrderStartVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 维修进度管理模块 Controller（R-01 ~ R-05）。
 *
 * <p>对应需求 FR-4-01 ~ FR-4-08，接口文档 4.5.1 ~ 4.5.5 节。</p>
 *
 * <p><b>为什么再新建一个 Controller，而不是往既有 Controller 里加方法？</b>
 * 因为本模块继续遵循「能新增就不修改」的原则：模块二 {@code RepairOrderController}
 * 与模块三 {@code RepairDispatchController} 同属前序模块的交付边界，
 * 本模块一字节都不动它们，从而保证前序模块的代码在整合工程中<b>逐字未变</b>。
 * 三个 Controller 的类级 {@code @RequestMapping} 同为 {@code /api/orders}，
 * Spring MVC 完全支持这种「同一基础路径、多 Controller 分担」的写法
 * （模块三 README 已就此说明，本模块沿用同一约定）。</p>
 *
 * <p><b>路径优先级说明</b>：本模块全部为两段式路径
 * （{@code /{orderId}/records}、{@code /{orderId}/start}、{@code /{orderId}/finish}、
 * {@code /{orderId}/duration}），与模块二的单段 {@code /{orderId}}、
 * 模块三的字面量路径（{@code /grab-pool} 等）均不冲突：
 * 段数不同或末段为字面量，Spring Boot 3 的 {@code PathPatternParser} 可精确分流。</p>
 *
 * <p><b>权限设计</b>（复用既有的 {@code JwtInterceptor} + {@code @RequireRole}，
 * <b>不新增任何鉴权组件、不修改拦截器一行代码</b>）：</p>
 * <ul>
 *   <li>类上<b>不加</b> {@code @RequireRole} —— 因为本 Controller 同时承载
 *       「全部登录用户可见」的 R-01 与「仅维修人员」的 R-02 / R-03 / R-04，
 *       以及「管理员 + 维修人员」的 R-05，统一改为<b>方法级</b>精确标注，
 *       利用拦截器「方法级优先」的既有规则，避免出现越权放行
 *       （与模块二 O-02 / O-03 与 O-01 并存的处理方式一致）；</li>
 *   <li>R-01 / R-05 的<b>数据范围</b>（居民本人 / 维修人员本人受派 / 管理员全部）
 *       在 Service 层强制注入，前端无法通过任何入参绕过（BR-4-01）；</li>
 *   <li>全部写操作方法在 Service 层还有第二道「角色兜底校验」与「数据归属校验」，
 *       即使前端被绕过也无法越权（NFR-S-03）。</li>
 * </ul>
 *
 * <p><b>用户身份获取方式</b>：Controller 层不接收任何操作人 ID 入参，
 * 一律由 Service 层通过 {@link com.community.repair.security.UserContext} 读取
 * JWT 解析出的登录信息，与模块零 / 一 / 二 / 三完全一致。</p>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class RepairProgressController {

    private final RepairProgressService repairProgressService;

    // ==================================================================================
    // 一、R-01 进度时间线查询 —— 全部登录用户（受数据范围约束）
    // ==================================================================================

    /**
     * R-01 进度时间线查询。
     *
     * <p>GET /api/orders/{orderId}/records　全部登录用户（服务端按角色限定数据范围）</p>
     *
     * <p>按 {@code recordTime} 升序返回该工单的全部进度节点，构成完整进度时间轴；
     * 可用 {@code recordType}（1~6）筛选某一类节点。
     * 居民仅可查看本人工单、维修人员仅可查看本人受派工单、管理员可查看全部，
     * 不符时返回 403；工单无任何记录时返回空数组而非 404。</p>
     *
     * <p>本方法<b>不加</b> {@code @RequireRole}：与模块二的 O-02 / O-03 一样，
     * 只要携带合法 JWT 即可访问，具体可见性由 Service 层的数据归属校验决定。</p>
     */
    @GetMapping("/{orderId}/records")
    public Result<List<OrderRecordVO>> listRecords(
            @PathVariable("orderId") Long orderId,
            @RequestParam(value = "recordType", required = false) Integer recordType) {
        return Result.success(ProgressConstants.MSG_QUERY_SUCCESS,
                repairProgressService.listRecords(orderId, recordType));
    }

    // ==================================================================================
    // 二、R-02 追加维修进度 —— 仅维修人员（且须为该工单受派人）
    // ==================================================================================

    /**
     * R-02 追加维修进度。
     *
     * <p>POST /api/orders/{orderId}/records　仅维修人员（且须为该工单受派人）</p>
     *
     * <p>维修过程中多次追加「到达现场 / 维修进度 / 更换配件」记录（可携带照片、配件与费用），
     * <b>不改变工单状态</b>（仍为 2 或 3）。「等待配件 / 暂停」按接口文档 Q5 的统一口径
     * 以「维修进度(3)」+ 文本承载。本次费用自动累加至工单 {@code total_cost}。</p>
     */
    @PostMapping("/{orderId}/records")
    @RequireRole(RoleEnum.REPAIR)
    public Result<OrderRecordCreateVO> appendRecord(@PathVariable("orderId") Long orderId,
                                                    @Valid @RequestBody OrderRecordCreateDTO dto) {
        return Result.success(ProgressConstants.MSG_RECORD_ADD_SUCCESS,
                repairProgressService.appendRecord(orderId, dto));
    }

    // ==================================================================================
    // 三、R-03 开始维修 —— 仅维修人员（且须为该工单受派人）
    // ==================================================================================

    /**
     * R-03 开始维修。
     *
     * <p>PUT /api/orders/{orderId}/start　仅维修人员（且须为该工单受派人）</p>
     *
     * <p>状态 <b>2 → 3</b>，并自动生成一条「到达现场」记录（{@code record_type = 2}），
     * 其记录时间即 BR-7-02「维修时长」的起点。仅状态 2 可调用，禁止跳级。</p>
     */
    @PutMapping("/{orderId}/start")
    @RequireRole(RoleEnum.REPAIR)
    public Result<OrderStartVO> startRepair(@PathVariable("orderId") Long orderId,
                                            @Valid @RequestBody(required = false) OrderStartDTO dto) {
        // 到场说明是可选字段，允许完全不传 body（例如前端不弹窗直接点「开始维修」）
        OrderStartDTO safeDto = dto == null ? new OrderStartDTO() : dto;
        return Result.success(ProgressConstants.MSG_START_SUCCESS,
                repairProgressService.startRepair(orderId, safeDto));
    }

    // ==================================================================================
    // 四、R-04 提交维修完成 —— 仅维修人员（且须为该工单受派人）
    // ==================================================================================

    /**
     * R-04 提交维修完成。
     *
     * <p>PUT /api/orders/{orderId}/finish　仅维修人员（且须为该工单受派人）</p>
     *
     * <p>状态 <b>3 → 4（待验收）</b>，记录 {@code finish_time}，
     * 并生成一条「完工」记录（{@code record_type = 5}）。维修结果说明必填 10~300 字（BR-4-04）。</p>
     *
     * <p>本接口是模块四流向<b>模块五「工单验收管理」</b>的唯一出口，
     * 落库后的 {@code status = 4} / {@code finishTime} / {@code totalCost}
     * 即下游待验收列表与验收操作的完整输入契约。</p>
     */
    @PutMapping("/{orderId}/finish")
    @RequireRole(RoleEnum.REPAIR)
    public Result<OrderFinishVO> finishRepair(@PathVariable("orderId") Long orderId,
                                              @Valid @RequestBody OrderFinishDTO dto) {
        return Result.success(ProgressConstants.MSG_FINISH_SUCCESS,
                repairProgressService.finishRepair(orderId, dto));
    }

    // ==================================================================================
    // 五、R-05 维修耗时统计 —— 系统管理员、维修人员
    // ==================================================================================

    /**
     * R-05 维修耗时统计。
     *
     * <p>GET /api/orders/{orderId}/duration　系统管理员、维修人员</p>
     *
     * <p>按 BR-7-02 口径输出派单 / 接单 / 维修 / 验收 / 全流程五项耗时（分钟），
     * 为统计分析模块（S-06）提供基础数据。未流转到的节点其时间为 null，对应时长也为 null。
     * 维修人员查询非本人受派工单时返回 403，该数据范围校验在 Service 层完成。</p>
     */
    @GetMapping("/{orderId}/duration")
    @RequireRole({RoleEnum.ADMIN, RoleEnum.REPAIR})
    public Result<OrderDurationVO> getOrderDuration(@PathVariable("orderId") Long orderId) {
        return Result.success(ProgressConstants.MSG_QUERY_SUCCESS,
                repairProgressService.getOrderDuration(orderId));
    }
}
