package com.community.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.repair.common.BusinessException;
import com.community.repair.common.PageResult;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.CategoryConstants;
import com.community.repair.constant.DispatchConstants;
import com.community.repair.constant.OrderConstants;
import com.community.repair.constant.UserConstants;
import com.community.repair.convert.DispatchConverter;
import com.community.repair.dto.BatchDispatchDTO;
import com.community.repair.dto.GrabPoolQueryDTO;
import com.community.repair.dto.GrabPublishDTO;
import com.community.repair.dto.OrderAcceptDTO;
import com.community.repair.dto.OrderAuditDTO;
import com.community.repair.dto.OrderDispatchDTO;
import com.community.repair.dto.OrderQueryDTO;
import com.community.repair.dto.OrderReassignDTO;
import com.community.repair.dto.OrderRejectDTO;
import com.community.repair.dto.PendingDispatchQueryDTO;
import com.community.repair.dto.WorkerWorkloadQueryDTO;
import com.community.repair.entity.RepairCategory;
import com.community.repair.entity.RepairDispatchLog;
import com.community.repair.entity.RepairOrder;
import com.community.repair.entity.RepairRecord;
import com.community.repair.entity.SysUser;
import com.community.repair.enums.AuditResultEnum;
import com.community.repair.enums.DispatchOpResultEnum;
import com.community.repair.enums.DispatchTypeEnum;
import com.community.repair.enums.RoleEnum;
import com.community.repair.mapper.RepairCategoryMapper;
import com.community.repair.mapper.RepairDispatchLogMapper;
import com.community.repair.mapper.RepairOrderMapper;
import com.community.repair.mapper.RepairOrderStatMapper;
import com.community.repair.mapper.RepairRecordMapper;
import com.community.repair.mapper.SysUserMapper;
import com.community.repair.security.LoginUser;
import com.community.repair.security.UserContext;
import com.community.repair.service.RepairDispatchService;
import com.community.repair.service.RepairOrderService;
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
import com.community.repair.vo.WorkerWorkloadStatVO;
import com.community.repair.vo.WorkerWorkloadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 工单派单调度模块 业务实现（模块三）。
 *
 * <p>本类集中承载需求文档 5.3 节 FR-3-01 ~ FR-3-12 与 BR-3-01 ~ BR-3-10 的全部业务规则，
 * 每个方法上方的注释都标注了对应规则编号，便于论文与答辩溯源。</p>
 *
 * <p><b>复用的基础组件（不重复创建）</b>：</p>
 * <ul>
 *   <li>统一响应体 {@code Result} / 分页结构 {@code PageResult} / 状态码字典 {@code ResultCode}
 *       + 全局异常处理器 {@code GlobalExceptionHandler}；</li>
 *   <li>鉴权与角色校验 {@code JwtInterceptor} / {@code @RequireRole} / {@code UserContext}
 *       —— <b>操作人身份一律通过 {@link UserContext#require()} 读取，不接受前端传入</b>；</li>
 *   <li>状态字典 {@code OrderStatusEnum} 与常量 {@code OrderConstants}
 *       —— <b>本模块不重复定义工单状态</b>；</li>
 *   <li>字段自动填充 {@code MyMetaObjectHandler}、分页插件 {@code PaginationInnerInterceptor}；</li>
 *   <li><b>模块二的 {@link RepairOrderService}</b>：D-11 待派单池直接复用它已实现的
 *       {@code pageOrders}（强制 {@code status = 0}），从而与 O-02 的列表口径、
 *       排序白名单、名称批量装配逻辑 100% 一致，<b>零重复开发</b>。</li>
 * </ul>
 *
 * <p><b>并发控制（NFR-R-01 / NFR-R-02 / BR-3-04）</b>：所有状态流转都采用
 * 「条件更新 + 受影响行数校验」的乐观锁写法
 * （{@code UPDATE ... WHERE order_id = ? AND status = ?}），
 * 受影响行数不为 1 即判定为并发冲突或状态不合法，统一返回 409。
 * 这一写法同时实现了「禁止跳步」：前置状态被固化在 WHERE 中，
 * 任何不在状态机内的迁移都不可能命中。</p>
 *
 * <p><b>与下游模块的衔接</b>：派单成功后 {@code repair_order} 的
 * {@code worker_id} / {@code dispatcher_id} / {@code dispatch_time} /
 * {@code dispatch_remark} 与状态 1（已派单）即构成模块四「维修进度管理」的完整输入，
 * <b>无需任何数据结构适配</b>。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepairDispatchServiceImpl implements RepairDispatchService {

    /** 工单主表：状态流转与派单快照的写入对象（模块二既有 Mapper，本模块只调用不修改） */
    private final RepairOrderMapper repairOrderMapper;

    /** 类目字典：抢单池列表展示类目名称（上游模块一的只读依赖） */
    private final RepairCategoryMapper repairCategoryMapper;

    /** 用户表：维修人员校验、姓名批量装配（模块零既有 Mapper） */
    private final SysUserMapper sysUserMapper;

    /** 派单记录表：派单链路的审计轨迹（本模块新增实体） */
    private final RepairDispatchLogMapper repairDispatchLogMapper;

    /** 维修记录表：D-08 接单、D-09 拒单需同步写入进度节点（本模块新增实体，模块四复用） */
    private final RepairRecordMapper repairRecordMapper;

    /** 工单统计 Mapper：D-01 负载聚合（本模块新增，只读） */
    private final RepairOrderStatMapper repairOrderStatMapper;

    /** 模块二业务接口：D-11 待派单池直接复用其 pageOrders，保证列表口径完全一致 */
    private final RepairOrderService repairOrderService;

    /**
     * 「已发布抢单池」判定 SQL 片段：存在一条 {@code dispatch_type = 3}（发布抢单池）的记录。
     *
     * <p>说明：{@code repair_order} 表未设专门的发布标记列，依据接口文档 Q3
     * 「派单轨迹唯一来源 = repair_dispatch_log」的口径，本模块以审计表推导该标记。</p>
     * <p>该 SQL 为<b>编译期常量</b>，不含任何用户输入，不存在注入风险。</p>
     */
    private static final String SQL_GRAB_PUBLISHED =
            "SELECT 1 FROM repair_dispatch_log gl WHERE gl.order_id = repair_order.order_id "
                    + "AND gl.deleted = 0 AND gl.dispatch_type = 3";

    /**
     * 「已被领取」判定 SQL 片段：存在一条派单(1) / 改派(2) / 抢单(4) 记录。
     * <p>与 {@link #SQL_GRAB_PUBLISHED} 组合后，即得到「发布过且尚未被任何人领走」的抢单池集合。</p>
     */
    private static final String SQL_GRAB_CONSUMED =
            "SELECT 1 FROM repair_dispatch_log gl WHERE gl.order_id = repair_order.order_id "
                    + "AND gl.deleted = 0 AND gl.dispatch_type IN (1, 2, 4)";

    // ==================================================================================
    // 一、审核环节（D-12）—— 8 → 0（通过）/ 8 → 9（驳回）
    // ==================================================================================

    /**
     * D-12 工单审核（通过 / 驳回）。
     *
     * <p>对应需求 FR-3-12、BR-3-01、BR-3-09、BR-3-10，接口文档 4.4.12。</p>
     *
     * <p><b>状态域严格为 8（待审核）</b>，与模块二的 O-01/O-06 一起构成
     * 「提交 → 审核 → 重新提交」闭环：</p>
     * <ul>
     *   <li><b>审核通过</b>：8 → 0（待派单），工单进入待派单池，可被 D-02 / D-03 / D-05 处理；
     *       {@code reject_reason} / {@code rejector_id} / {@code reject_time} 保持 {@code NULL}；</li>
     *   <li><b>审核驳回</b>：8 → 9（已驳回），<b>必须填写驳回原因（5~200 字，BR-3-09）</b>，
     *       同时写入驳回三要素快照；已驳复工单不进入待派单池、不参与派单排序。</li>
     * </ul>
     *
     * <p><b>驳回结果如何反馈给提交工单的居民？</b>本方法把状态置为 9 并写入驳回原因后，
     * 居民端「我的报修」列表（O-02）与工单详情（O-03）每次都<b>实时查库</b>，
     * 会立即呈现「已驳回」状态标签与驳回原因、驳回人、驳回时间四要素，
     * 同时详情返回的 {@code canOperate.canResubmit = true} 引导居民调用
     * O-06 修改后重新提交（状态回到 8）——无需引入额外的消息表或推送通道，
     * 与模块二既有的实时联动设计保持一致。</p>
     *
     * <p><b>审核不写入 {@code repair_dispatch_log}</b>：审核不是派单动作，
     * 依据接口文档 4.4.12 与附录 A「两条链路在状态机上串联、在审计表上分离」。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderAuditVO auditOrder(Long orderId, OrderAuditDTO dto) {
        LoginUser current = requireAdmin();

        // 审核结果取值域校验：只能是 1（通过）或 0（驳回）
        AuditResultEnum result = AuditResultEnum.ofCode(dto.getAuditResult());
        if (result == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, DispatchConstants.MSG_AUDIT_RESULT_INVALID);
        }

        RepairOrder order = selectExistingOrder(orderId);
        // BR-3-01：允许审核的状态严格为 8 待审核（禁止跳步）
        if (!isStatus(order, OrderConstants.STATUS_PENDING_AUDIT)) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_AUDITABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean passed = AuditResultEnum.PASS == result;

        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        // 乐观锁：前置状态固化在 WHERE 中，受影响行数 != 1 即判定并发冲突 / 状态已变
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_AUDIT);

        if (passed) {
            // 8 → 0：进入待派单池；驳回三要素保持 NULL（不写入，维持既有 null 值）
            wrapper.set(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_DISPATCH);
        } else {
            // 8 → 9：BR-3-09 强制填写驳回原因
            String rejectReason = normalizeRejectReason(dto.getRejectReason());
            wrapper.set(RepairOrder::getStatus, OrderConstants.STATUS_REJECTED)
                    .set(RepairOrder::getRejectReason, rejectReason)
                    .set(RepairOrder::getRejectorId, current.getUserId())
                    .set(RepairOrder::getRejectTime, now);
            order.setRejectReason(rejectReason);
            order.setRejectorId(current.getUserId());
            order.setRejectTime(now);
        }
        wrapper.set(RepairOrder::getUpdateTime, now);

        if (repairOrderMapper.update(null, wrapper) != 1) {
            // 并发场景：另一管理员已抢先审核，或居民在审核期间撤销了工单
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_AUDITABLE);
        }

        order.setStatus(passed ? OrderConstants.STATUS_PENDING_DISPATCH : OrderConstants.STATUS_REJECTED);
        order.setUpdateTime(now);

        log.info("管理员 {} 审核工单：orderId={}, auditResult={}, 状态 8 → {}",
                current.getUserId(), orderId, result.getCode(), order.getStatus());
        return DispatchConverter.toAuditVO(order, result, passed ? null : current.getRealName());
    }

    // ==================================================================================
    // 二、派单链路 —— 单条派单（D-02）
    // ==================================================================================

    /**
     * D-02 派单给维修人员。
     *
     * <p>对应需求 FR-3-02、BR-3-01、BR-3-03、BR-3-08，接口文档 4.4.2。</p>
     *
     * <p>将<b>已审核通过</b>的待派单工单（状态 0）指派给指定维修人员：</p>
     * <ol>
     *   <li>BR-3-01：仅状态 0 可派单；若状态为 8，按接口文档给出专门提示
     *       「该工单尚未通过审核，请先审核」；</li>
     *   <li>BR-3-03：目标人员必须 {@code role = repair} 且 {@code status = 1}（启用）；</li>
     *   <li>成功后写入 {@code worker_id}、{@code dispatcher_id}、{@code dispatch_time}、
     *       {@code dispatch_remark}，状态由 0 置为 1；</li>
     *   <li>BR-3-08：写入一条 {@code dispatch_type = 1}（派单）的派单记录。</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDispatchVO dispatchOrder(Long orderId, OrderDispatchDTO dto) {
        LoginUser current = requireAdmin();

        RepairOrder order = selectExistingOrder(orderId);
        // BR-3-01：仅状态 0 待派单可派单
        assertPendingDispatch(order);
        // BR-3-03：目标人员必须是启用的维修人员
        SysUser worker = requireEnabledRepairWorker(dto.getWorkerId());

        LocalDateTime now = LocalDateTime.now();
        String remark = normalizeRemark(dto.getDispatchRemark(), "派单备注");

        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_DISPATCH)
                .set(RepairOrder::getStatus, OrderConstants.STATUS_DISPATCHED)
                .set(RepairOrder::getWorkerId, worker.getUserId())
                .set(RepairOrder::getDispatcherId, current.getUserId())
                .set(RepairOrder::getDispatchTime, now)
                .set(RepairOrder::getDispatchRemark, remark)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, wrapper) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_DISPATCHABLE);
        }

        // BR-3-08：写入派单审计记录（原受派人无，故 fromWorkerId 传 null）
        insertDispatchLog(orderId, DispatchTypeEnum.DISPATCH, current.getUserId(),
                null, worker.getUserId(), now, remark);

        order.setStatus(OrderConstants.STATUS_DISPATCHED);
        order.setWorkerId(worker.getUserId());
        order.setDispatcherId(current.getUserId());
        order.setDispatchTime(now);
        order.setDispatchRemark(remark);
        order.setUpdateTime(now);

        log.info("管理员 {} 派单成功：orderId={}, workerId={}, 状态 0 → 1",
                current.getUserId(), orderId, worker.getUserId());
        return DispatchConverter.toDispatchVO(order, worker.getRealName());
    }

    // ==================================================================================
    // 三、派单链路 —— 批量派单（D-03）
    // ==================================================================================

    /**
     * D-03 批量派单。
     *
     * <p>对应需求 FR-3-03、NFR-R-01，接口文档 4.4.3。</p>
     *
     * <p>勾选多条待派单工单，一次性指派给同一维修人员。<b>单次最多 50 条</b>，
     * 参与批量派单的工单状态必须均为 0（待派单）。</p>
     *
     * <p><b>事务与失败处理策略</b>：整批在<b>单一事务</b>中逐单执行，
     * 单条工单的「工单状态更新 + 派单记录写入」保持原子（NFR-R-01）；
     * 状态不合法的工单<b>不阻断</b>其余工单，而是记录进返回的失败明细，
     * 因此接口可能返回「部分成功」的结果 —— 这与接口文档 4.4.3 的响应示例
     * （{@code successCount = 3}、{@code failCount = 1}）及状态码说明
     * 「200 = 全部成功或部分失败（失败明细在 data 中）」完全一致。</p>
     *
     * <p>若执行过程中出现数据库层面的意外异常，事务整体回滚，本批派单全部不生效，
     * 保证不会出现「工单改了状态却没留审计记录」的半成品数据。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchDispatchVO batchDispatch(BatchDispatchDTO dto) {
        LoginUser current = requireAdmin();

        List<Long> orderIds = dto.getOrderIds();
        if (orderIds == null || orderIds.isEmpty()
                || orderIds.size() > DispatchConstants.BATCH_MAX_COUNT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, DispatchConstants.MSG_BATCH_SIZE_INVALID);
        }
        // BR-3-03：目标人员校验（全批共用同一人）
        SysUser worker = requireEnabledRepairWorker(dto.getWorkerId());

        String remark = normalizeRemark(dto.getDispatchRemark(), "派单备注");
        // 去重：防止同一工单在一次请求中被重复派单
        Set<Long> distinctIds = new LinkedHashSet<>(orderIds);

        BatchDispatchVO result = new BatchDispatchVO();
        List<BatchDispatchVO.FailItem> failList = new ArrayList<>();

        for (Long orderId : distinctIds) {
            RepairOrder order = orderId == null ? null : repairOrderMapper.selectById(orderId);
            if (order == null) {
                failList.add(new BatchDispatchVO.FailItem(orderId, null, OrderConstants.MSG_ORDER_NOT_FOUND));
                continue;
            }
            // 状态前置校验：失败的工单不影响其余工单
            if (!isStatus(order, OrderConstants.STATUS_PENDING_DISPATCH)) {
                failList.add(new BatchDispatchVO.FailItem(orderId, order.getOrderNo(), pendingDispatchFailReason(order)));
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(RepairOrder::getOrderId, orderId)
                    .eq(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_DISPATCH)
                    .set(RepairOrder::getStatus, OrderConstants.STATUS_DISPATCHED)
                    .set(RepairOrder::getWorkerId, worker.getUserId())
                    .set(RepairOrder::getDispatcherId, current.getUserId())
                    .set(RepairOrder::getDispatchTime, now)
                    .set(RepairOrder::getDispatchRemark, remark)
                    .set(RepairOrder::getUpdateTime, now);
            if (repairOrderMapper.update(null, wrapper) != 1) {
                // 并发：该单在本批处理期间已被其他管理员派单
                failList.add(new BatchDispatchVO.FailItem(orderId, order.getOrderNo(),
                        DispatchConstants.MSG_STATUS_NOT_DISPATCHABLE));
                continue;
            }

            insertDispatchLog(orderId, DispatchTypeEnum.DISPATCH, current.getUserId(),
                    null, worker.getUserId(), now, remark);
            result.setSuccessCount(result.getSuccessCount() + 1);
        }

        result.setFailList(failList);
        result.setFailCount(failList.size());
        log.info("管理员 {} 批量派单完成：workerId={}, 成功 {} 条，失败 {} 条",
                current.getUserId(), worker.getUserId(), result.getSuccessCount(), result.getFailCount());
        return result;
    }

    // ==================================================================================
    // 四、派单链路 —— 改派（D-04）
    // ==================================================================================

    /**
     * D-04 改派。
     *
     * <p>对应需求 FR-3-04、BR-3-02、BR-3-08、BR-8-19，接口文档 4.4.4。</p>
     *
     * <p>对<b>已派单（1）且维修人员尚未接单（{@code accept_time} 为空）</b>的工单重新指派他人。
     * 原受派信息被覆盖，同时写入一条 {@code dispatch_type = 2} 的派单记录，
     * 通过 {@code from_worker_id} / {@code to_worker_id} 成对还原改派链条（BR-3-08）。</p>
     *
     * <p><b>BR-8-19 联动场景</b>：管理员禁用某维修人员账号后，
     * 其名下在途工单即通过本接口改派他人。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderReassignVO reassignOrder(Long orderId, OrderReassignDTO dto) {
        LoginUser current = requireAdmin();

        RepairOrder order = selectExistingOrder(orderId);
        // BR-3-02：仅状态 1（已派单）可改派
        if (!isStatus(order, OrderConstants.STATUS_DISPATCHED)) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_REASSIGNABLE);
        }
        // BR-3-02：维修人员一旦接单（accept_time 有值）即不可再改派
        if (order.getAcceptTime() != null) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_ALREADY_ACCEPTED);
        }
        // BR-3-03：新目标人员必须是启用的维修人员
        SysUser newWorker = requireEnabledRepairWorker(dto.getWorkerId());

        Long fromWorkerId = order.getWorkerId();
        LocalDateTime now = LocalDateTime.now();
        String remark = normalizeRemark(dto.getDispatchRemark(), "改派说明");

        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_DISPATCHED)
                // 并发保护：若此刻维修人员已接单，则本条更新不命中，返回 409
                .isNull(RepairOrder::getAcceptTime)
                .set(RepairOrder::getWorkerId, newWorker.getUserId())
                .set(RepairOrder::getDispatcherId, current.getUserId())
                .set(RepairOrder::getDispatchTime, now)
                .set(RepairOrder::getDispatchRemark, remark)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, wrapper) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_REASSIGNABLE);
        }

        // BR-3-08：改派记录必须成对记录原受派人与新受派人
        insertDispatchLog(orderId, DispatchTypeEnum.REASSIGN, current.getUserId(),
                fromWorkerId, newWorker.getUserId(), now, remark);

        order.setWorkerId(newWorker.getUserId());
        order.setDispatcherId(current.getUserId());
        order.setDispatchTime(now);
        order.setDispatchRemark(remark);
        order.setUpdateTime(now);

        log.info("管理员 {} 改派工单：orderId={}, from={}, to={}",
                current.getUserId(), orderId, fromWorkerId, newWorker.getUserId());
        return DispatchConverter.toReassignVO(order, newWorker.getRealName());
    }

    // ==================================================================================
    // 五、派单链路 —— 发布抢单池（D-05）与抢单（D-07）
    // ==================================================================================

    /**
     * D-05 发布抢单池。
     *
     * <p>对应需求 FR-3-05、BR-3-01、BR-3-08，接口文档 4.4.5。</p>
     *
     * <p>把状态 0（待派单）工单发布为可抢单状态。发布后工单<b>仍保持状态 0</b>
     * （不产生状态迁移），但会被 D-06 抢单池列表检索到，
     * 并写入一条 {@code dispatch_type = 3} 的派单记录（BR-3-08）。</p>
     *
     * <p><b>「已发布」标记如何持久化？</b>{@code repair_order} 表未设专门的发布标记字段，
     * 依据接口文档 Q3「派单轨迹唯一来源 = {@code repair_dispatch_log}」的口径，
     * 本方法以审计表推导标记：<b>存在 {@code dispatch_type = 3} 且其后不存在
     * {@code dispatch_type ∈ (1,2,4)} 的记录</b>，即视为「当前在抢单池中」。
     * 这样既不新增表字段（沿用方案 A 的建表脚本），又天然满足
     * 「重复发布返回 409」与「被领走后自动出池」两条规则。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrabPublishVO publishGrabPool(Long orderId, GrabPublishDTO dto) {
        LoginUser current = requireAdmin();

        RepairOrder order = selectExistingOrder(orderId);
        // BR-3-01：仅状态 0 可发布
        if (!isStatus(order, OrderConstants.STATUS_PENDING_DISPATCH)) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_PUBLISHABLE);
        }
        // 重复发布拦截
        if (isInGrabPool(orderId)) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_ALREADY_IN_GRAB_POOL);
        }

        LocalDateTime now = LocalDateTime.now();
        String remark = normalizeRemark(dto.getDispatchRemark(), "发布说明");

        // 发布动作不改变工单状态，因此不更新 repair_order；
        // 但需二次确认状态仍为 0，避免与派单 / 撤销动作产生时间窗竞态
        RepairOrder latest = repairOrderMapper.selectById(orderId);
        if (latest == null || !isStatus(latest, OrderConstants.STATUS_PENDING_DISPATCH)) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_PUBLISHABLE);
        }

        // BR-3-08：发布抢单池同样属于派单链路的审计动作，独立成行
        insertDispatchLog(orderId, DispatchTypeEnum.PUBLISH_GRAB, current.getUserId(),
                null, null, now, remark);

        log.info("管理员 {} 发布抢单池：orderId={}, 状态保持 0", current.getUserId(), orderId);
        return DispatchConverter.toGrabPublishVO(latest, now);
    }

    /**
     * D-07 抢单。
     *
     * <p>对应需求 FR-3-08、BR-3-04、BR-3-08，接口文档 4.4.7。</p>
     *
     * <p>维修人员在抢单池中领取工单，系统按<b>先到先得</b>原则锁定：
     * 状态由 0 置为 1（已派单）并写入 {@code worker_id}，
     * 成功后写入一条 {@code dispatch_type = 4} 的派单记录。</p>
     *
     * <p><b>并发唯一（BR-3-04）</b>：条件更新
     * {@code WHERE order_id = ? AND status = 0} 保证同一时刻只有一个维修人员能锁定成功，
     * 未命中者返回 409「该工单已被其他维修人员领取」。</p>
     *
     * <p><b>为何不写 {@code dispatcher_id}？</b>该字段语义为「派单管理员」，
     * 抢单由维修人员自发发起、不存在派单管理员，故保持为空，
     * 与接口文档 4.4.7 的出参（不含 dispatcherId）一致。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderGrabVO grabOrder(Long orderId) {
        LoginUser current = requireRepair();

        RepairOrder order = selectExistingOrder(orderId);
        // 状态与「已发布」双重前置校验：未发布或状态不为 0 一律拒绝
        if (!isStatus(order, OrderConstants.STATUS_PENDING_DISPATCH) || !isInGrabPool(orderId)) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_NOT_GRABBABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_DISPATCH)   // 先到先得锁
                .set(RepairOrder::getStatus, OrderConstants.STATUS_DISPATCHED)
                .set(RepairOrder::getWorkerId, current.getUserId())
                .set(RepairOrder::getDispatchTime, now)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, wrapper) != 1) {
            // BR-3-04：并发抢单冲突
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_GRABBED_BY_OTHER);
        }

        insertDispatchLog(orderId, DispatchTypeEnum.GRAB, current.getUserId(),
                null, current.getUserId(), now, null);

        order.setStatus(OrderConstants.STATUS_DISPATCHED);
        order.setWorkerId(current.getUserId());
        order.setDispatchTime(now);
        order.setUpdateTime(now);

        log.info("维修人员 {} 抢单成功：orderId={}, 状态 0 → 1", current.getUserId(), orderId);
        return DispatchConverter.toGrabVO(order);
    }

    // ==================================================================================
    // 六、派单链路 —— 接单（D-08）与拒单（D-09）
    // ==================================================================================

    /**
     * D-08 接单。
     *
     * <p>对应需求 FR-3-09、FR-4-02、BR-3-08，接口文档 4.4.8。</p>
     *
     * <p>维修人员确认接受分派：记录 {@code accept_time}，状态由 1 置为 2（已接单），
     * 自动写入一条类型为「接单」的维修记录（{@code repair_record.record_type = 1}）
     * 形成进度时间线的首个节点，并写入一条 {@code dispatch_type = 5} 的派单记录。</p>
     *
     * <p><b>数据归属校验（BR-4-01）</b>：工单必须指派给当前登录的维修人员，
     * 否则返回 403「无权限操作该工单」——与模块二「数据归属以服务端判定为准」一致。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderAcceptVO acceptOrder(Long orderId, OrderAcceptDTO dto) {
        LoginUser current = requireRepair();

        RepairOrder order = selectExistingOrder(orderId);
        assertAssignedWorker(order, current);

        // 状态域：仅状态 1（已派单）可接单
        if (!isStatus(order, OrderConstants.STATUS_DISPATCHED)) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_ACCEPTABLE);
        }
        // 重复接单拦截
        if (order.getAcceptTime() != null) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_ALREADY_ACCEPTED_ONCE);
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_DISPATCHED)
                .isNull(RepairOrder::getAcceptTime)
                .set(RepairOrder::getStatus, OrderConstants.STATUS_ACCEPTED)
                .set(RepairOrder::getAcceptTime, now)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, wrapper) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_ALREADY_ACCEPTED_ONCE);
        }

        // 进度时间线首个节点：record_type = 1 接单
        String content = StringUtils.hasText(dto.getRemark())
                ? dto.getRemark().trim()
                : DispatchConstants.DEFAULT_ACCEPT_CONTENT;
        Long recordId = insertRecord(orderId, current.getUserId(),
                DispatchConstants.RECORD_TYPE_ACCEPT, content, now);

        // BR-3-08：接单同样写入派单审计轨迹
        insertDispatchLog(orderId, DispatchTypeEnum.ACCEPT, current.getUserId(),
                null, current.getUserId(), now, StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);

        order.setStatus(OrderConstants.STATUS_ACCEPTED);
        order.setAcceptTime(now);
        order.setUpdateTime(now);

        log.info("维修人员 {} 接单成功：orderId={}, 状态 1 → 2, recordId={}",
                current.getUserId(), orderId, recordId);
        return DispatchConverter.toAcceptVO(order, recordId);
    }

    /**
     * D-09 拒单。
     *
     * <p>对应需求 FR-3-10、BR-3-05、BR-3-06、BR-3-08，接口文档 4.4.9。</p>
     *
     * <p>维修人员拒绝接单，工单<b>回退至状态 0（待派单）</b>并清空原受派人信息
     * （{@code worker_id} / {@code dispatcher_id} / {@code dispatch_time} /
     * {@code dispatch_remark} 一并置空，恢复为「尚未派单」的干净状态，
     * 使工单可被重新派单），同时：</p>
     * <ul>
     *   <li>留存一条类型为「转派退回」的维修记录（{@code record_type = 6}），承载拒单原因；</li>
     *   <li>写入一条 {@code dispatch_type = 6} 的派单记录，拒单原因写入其 {@code remark}
     *       —— 原派单信息虽已从工单快照中清空，但完整留痕于审计表
     *       （这正是 BR-3-08「保留完整审计轨迹」的价值所在）。</li>
     * </ul>
     *
     * <p><b>语义区分</b>（接口文档 Q9）：本接口的「拒单」是维修人员行为，
     * 与 D-12 管理员「审核驳回」不是同一环节。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderRejectVO rejectOrder(Long orderId, OrderRejectDTO dto) {
        LoginUser current = requireRepair();

        RepairOrder order = selectExistingOrder(orderId);
        assertAssignedWorker(order, current);

        // 状态域：仅状态 1（已派单）可拒单；已接单（2）之后不可拒单
        if (!isStatus(order, OrderConstants.STATUS_DISPATCHED)) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_REJECTABLE);
        }
        // BR-3-05：拒单原因 5~200 字（@Size 已拦一层，这里兜住纯空格）
        String reason = normalizeRejectOrderReason(dto.getRejectReason());

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_DISPATCHED)
                // 1 → 0 回退，并清空当前有效派单快照
                .set(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_DISPATCH)
                .set(RepairOrder::getWorkerId, null)
                .set(RepairOrder::getDispatcherId, null)
                .set(RepairOrder::getDispatchTime, null)
                .set(RepairOrder::getDispatchRemark, null)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, wrapper) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_STATUS_NOT_REJECTABLE);
        }

        // BR-3-06：留存「转派退回」维修记录
        Long recordId = insertRecord(orderId, current.getUserId(),
                DispatchConstants.RECORD_TYPE_TRANSFER_BACK, reason, now);

        // BR-3-08：写入拒单审计记录，to_worker_id 记录拒单的维修人员，remark 承载拒单原因
        insertDispatchLog(orderId, DispatchTypeEnum.REJECT, current.getUserId(),
                current.getUserId(), null, now, reason);

        order.setStatus(OrderConstants.STATUS_PENDING_DISPATCH);
        order.setWorkerId(null);
        order.setDispatcherId(null);
        order.setDispatchTime(null);
        order.setDispatchRemark(null);
        order.setUpdateTime(now);

        log.info("维修人员 {} 拒单成功：orderId={}, 状态 1 → 0, recordId={}, 原因={}",
                current.getUserId(), orderId, recordId, reason);
        return DispatchConverter.toRejectVO(order, recordId);
    }

    // ==================================================================================
    // 七、查询类 —— 抢单池（D-06）、派单记录（D-10）、待派单池（D-11）、负载视图（D-01）
    // ==================================================================================

    /**
     * D-06 抢单池列表。
     *
     * <p>对应需求 FR-3-08、BR-3-07，接口文档 4.4.6。</p>
     *
     * <p>集合口径：{@code status = 0}（待派单）<b>且已发布至抢单池且尚未被领取</b>。
     * 后两个条件由服务端通过 {@code EXISTS / NOT EXISTS} 子查询强制注入，
     * 前端无法通过任何入参绕过。</p>
     *
     * <p>排序：按紧急程度降序、提交时间降序（接口文档 4.4.6 明确要求两者均为倒序，
     * 使紧急工单与最新工单优先被维修人员看到）。</p>
     */
    @Override
    public PageResult<GrabPoolItemVO> pageGrabPool(GrabPoolQueryDTO query) {
        // 需登录；维修人员角色由 Controller 的 @RequireRole 强制保证
        UserContext.require();

        int pageNum = (query.getPageNum() == null || query.getPageNum() < 1) ? 1 : query.getPageNum();
        int pageSize = (query.getPageSize() == null || query.getPageSize() < 1)
                ? DispatchConstants.DEFAULT_PAGE_SIZE
                : Math.min(query.getPageSize(), DispatchConstants.MAX_PAGE_SIZE);

        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_DISPATCH);
        // 抢单池集合条件：发布过（EXISTS type=3）且未被领走（NOT EXISTS type IN 1/2/4）
        wrapper.exists(SQL_GRAB_PUBLISHED);
        wrapper.notExists(SQL_GRAB_CONSUMED);
        wrapper.eq(query.getCategoryId() != null, RepairOrder::getCategoryId, query.getCategoryId());
        wrapper.eq(query.getUrgency() != null, RepairOrder::getUrgency, query.getUrgency());

        if (StringUtils.hasText(query.getAddressKeyword())) {
            String keyword = query.getAddressKeyword().trim();
            if (keyword.length() > DispatchConstants.ADDRESS_KEYWORD_MAX_LENGTH) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "地址关键字长度不能超过 50 个字符");
            }
            wrapper.like(RepairOrder::getAddress, keyword);
        }

        // BR-3-07：紧急工单优先；同紧急程度下最新提交的在前
        wrapper.orderByDesc(RepairOrder::getUrgency).orderByDesc(RepairOrder::getCreateTime);

        Page<RepairOrder> page = repairOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        // 批量装配类目名称，避免 N+1
        Map<Long, String> categoryNameMap = loadCategoryNameMap();
        return PageResult.of(page, order -> DispatchConverter.toGrabPoolItemVO(
                order, categoryNameMap.get(order.getCategoryId())));
    }

    /**
     * D-10 派单记录查询。
     *
     * <p>对应需求 FR-3-07、BR-3-08，接口文档 4.4.10。</p>
     *
     * <p>数据来源为 {@code repair_dispatch_log}，按 {@code dispatch_time} 升序返回该工单的
     * 完整派单轨迹；审核环节（D-12）不写入本表，故轨迹中不会出现审核动作。</p>
     *
     * <p><b>数据范围</b>：管理员可查任意工单；维修人员仅可查<b>当前受派</b>的工单，
     * 否则返回 403（接口文档 4.4.10 状态码说明）。</p>
     */
    @Override
    public List<DispatchRecordVO> listDispatchRecords(Long orderId) {
        LoginUser current = UserContext.require();
        RepairOrder order = selectExistingOrder(orderId);

        boolean isAdmin = RoleEnum.ADMIN.getCode().equalsIgnoreCase(current.getRole());
        if (!isAdmin) {
            boolean isAssignedWorker = RoleEnum.REPAIR.getCode().equalsIgnoreCase(current.getRole())
                    && Objects.equals(order.getWorkerId(), current.getUserId());
            if (!isAssignedWorker) {
                log.warn("越权查看派单记录被拦截：userId={}, role={}, orderId={}",
                        current.getUserId(), current.getRole(), orderId);
                throw new BusinessException(ResultCode.FORBIDDEN,
                        DispatchConstants.MSG_NO_PERMISSION_DISPATCH_RECORD);
            }
        }

        List<RepairDispatchLog> logs = repairDispatchLogMapper.selectList(
                new LambdaQueryWrapper<RepairDispatchLog>()
                        .eq(RepairDispatchLog::getOrderId, orderId)
                        .orderByAsc(RepairDispatchLog::getDispatchTime)
                        .orderByAsc(RepairDispatchLog::getDispatchId));
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        // 一次性收集涉及的全部用户 ID（操作人 + 原受派人 + 新受派人），批量装姓名与角色
        Set<Long> userIds = new HashSet<>();
        for (RepairDispatchLog item : logs) {
            addIfNotNull(userIds, item.getOperatorId());
            addIfNotNull(userIds, item.getFromWorkerId());
            addIfNotNull(userIds, item.getToWorkerId());
        }
        Map<Long, SysUser> userMap = loadUserMap(userIds);

        List<DispatchRecordVO> result = new ArrayList<>(logs.size());
        for (RepairDispatchLog item : logs) {
            SysUser operator = userMap.get(item.getOperatorId());
            result.add(DispatchConverter.toDispatchRecordVO(
                    item,
                    nameOf(userMap, item.getOperatorId()),
                    operator == null ? null : operator.getRole(),
                    nameOf(userMap, item.getFromWorkerId()),
                    nameOf(userMap, item.getToWorkerId())));
        }
        return result;
    }

    /**
     * D-11 待派单池快捷查询。
     *
     * <p>对应需求 FR-3-01、BR-3-07，接口文档 4.4.11。</p>
     *
     * <p>等价于 {@code GET /api/orders?status=0}，默认按紧急程度降序、提交时间升序
     * （紧急工单置顶）。</p>
     *
     * <p><b>实现要点：直接复用模块二已实现的查询能力。</b>本方法把入参转换为模块二的
     * {@link OrderQueryDTO} 并强制 {@code status = 0}，再委托
     * {@link RepairOrderService#pageOrders} 执行。这样做的收益是：
     * 分页参数保护、越权筛选拦截、排序字段白名单、关键字模糊匹配、
     * 类目/居民/维修人员名称的批量装配（避免 N+1）等逻辑<b>全部复用同一份实现</b>，
     * 既满足接口文档「返回参数字段同 O-02 列表元素」的要求，
     * 又从根本上杜绝了「两套列表口径不一致」的隐患。</p>
     */
    @Override
    public PageResult<OrderListVO> pagePendingDispatch(PendingDispatchQueryDTO query) {
        UserContext.require();

        OrderQueryDTO delegate = new OrderQueryDTO();
        delegate.setPageNum(query.getPageNum());
        delegate.setPageSize(query.getPageSize());
        // 强制锁定为待派单，前端无法通过任何方式改变本视图的状态范围
        delegate.setStatus(String.valueOf(OrderConstants.STATUS_PENDING_DISPATCH));
        delegate.setCategoryId(query.getCategoryId());
        delegate.setUrgency(query.getUrgency());
        delegate.setStartTime(query.getStartTime());
        delegate.setEndTime(query.getEndTime());
        delegate.setOrderBy(query.getOrderBy());
        delegate.setOrderDir(query.getOrderDir());

        return repairOrderService.pageOrders(delegate);
    }

    /**
     * D-01 维修人员负载视图。
     *
     * <p>对应需求 FR-3-06，接口文档 4.4.1，数据库 V2.0 附录 C2。</p>
     *
     * <p>返回全部维修人员（可按工种、账号状态筛选）及其工单量指标，
     * 用于辅助管理员<b>均衡派单</b>，也是前端「派单弹窗」选人时的数据来源。</p>
     *
     * <p><b>统计口径</b>：在途量按状态 1~4 统计，处理中按 2~3，完成量按 5~6；
     * 待审核(8) / 已驳回(9) / 已撤销(7) 未进入派单链路，<b>不计入任何工作量指标</b>。
     * 口径常量集中在 {@code DispatchConstants.WORKLOAD_*_STATUSES}，
     * 并作为参数传入聚合 SQL，保证全项目只有一处定义。</p>
     *
     * <p><b>性能</b>：先用一条聚合 SQL 拿到「有工单的维修人员」的工单量，
     * 再与人员档案在内存中按 ID 合并（无工单的新人补 0），
     * 全程只查两张表、不产生 N+1。</p>
     */
    @Override
    public List<WorkerWorkloadVO> listWorkerWorkload(WorkerWorkloadQueryDTO query) {
        UserContext.require();

        LambdaQueryWrapper<SysUser> workerWrapper = new LambdaQueryWrapper<>();
        workerWrapper.eq(SysUser::getRole, RoleEnum.REPAIR.getCode());
        // 默认仅返回账号正常的维修人员（停用账号不可被派单，BR-3-03）
        if (query.getOnlyEnabled() == null || Boolean.TRUE.equals(query.getOnlyEnabled())) {
            workerWrapper.eq(SysUser::getStatus, UserConstants.STATUS_ENABLED);
        }
        if (StringUtils.hasText(query.getWorkerType())) {
            workerWrapper.eq(SysUser::getWorkerType, query.getWorkerType().trim());
        }
        workerWrapper.orderByAsc(SysUser::getUserId);

        List<SysUser> workers = sysUserMapper.selectList(workerWrapper);
        if (workers.isEmpty()) {
            return Collections.emptyList();
        }

        // 一条 GROUP BY SQL 算出三位指标
        List<WorkerWorkloadStatVO> stats = repairOrderStatMapper.selectWorkerWorkloadStat(
                DispatchConstants.WORKLOAD_PENDING_STATUSES,
                DispatchConstants.WORKLOAD_PROCESSING_STATUSES,
                DispatchConstants.WORKLOAD_FINISHED_STATUSES);
        Map<Long, WorkerWorkloadStatVO> statMap = new HashMap<>();
        for (WorkerWorkloadStatVO stat : stats) {
            statMap.put(stat.getWorkerId(), stat);
        }

        List<WorkerWorkloadVO> result = new ArrayList<>(workers.size());
        for (SysUser worker : workers) {
            result.add(DispatchConverter.toWorkloadVO(worker, statMap.get(worker.getUserId())));
        }
        return result;
    }

    // ==================================================================================
    // 八、私有方法 —— 身份与权限
    // ==================================================================================

    /**
     * 取得当前登录管理员身份。
     * <p>兜底防御：{@code @RequireRole(ADMIN)} 已在拦截器层拦截非管理员，
     * 此处再校验一次，确保 Service 被其它入口调用时依然安全（NFR-S-03）。</p>
     */
    private LoginUser requireAdmin() {
        LoginUser current = UserContext.require();
        if (!RoleEnum.ADMIN.getCode().equalsIgnoreCase(current.getRole())) {
            log.warn("非管理员越权调用派单调度接口被拦截：userId={}, role={}",
                    current.getUserId(), current.getRole());
            throw new BusinessException(ResultCode.FORBIDDEN, DispatchConstants.MSG_ROLE_NOT_ADMIN);
        }
        return current;
    }

    /** 取得当前登录维修人员身份（同上的兜底防御） */
    private LoginUser requireRepair() {
        LoginUser current = UserContext.require();
        if (!RoleEnum.REPAIR.getCode().equalsIgnoreCase(current.getRole())) {
            log.warn("非维修人员越权调用接单 / 拒单 / 抢单接口被拦截：userId={}, role={}",
                    current.getUserId(), current.getRole());
            throw new BusinessException(ResultCode.FORBIDDEN, DispatchConstants.MSG_ROLE_NOT_REPAIR);
        }
        return current;
    }

    /**
     * 数据归属校验：工单必须指派给当前登录的维修人员（BR-4-01）。
     * <p>不通过抛 403，且不泄露工单以外的任何信息。</p>
     */
    private void assertAssignedWorker(RepairOrder order, LoginUser current) {
        if (!Objects.equals(order.getWorkerId(), current.getUserId())) {
            log.warn("维修人员越权操作非本人受派工单：userId={}, orderId={}, 实际受派人={}",
                    current.getUserId(), order.getOrderId(), order.getWorkerId());
            throw new BusinessException(ResultCode.FORBIDDEN, OrderConstants.MSG_NO_PERMISSION_OPERATE);
        }
    }

    // ==================================================================================
    // 九、私有方法 —— 状态与目标对象校验
    // ==================================================================================

    /** 按主键查询未删除的工单，不存在时抛出 404（{@code @TableLogic} 自动追加 deleted = 0） */
    private RepairOrder selectExistingOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "工单ID不能为空");
        }
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, OrderConstants.MSG_ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 派单前置状态断言（BR-3-01）。
     * <p>状态为 8 待审核时给出接口文档 4.4.2 专门约定的话术
     * 「该工单尚未通过审核，请先审核」，其余非 0 状态给通用话术。</p>
     */
    private void assertPendingDispatch(RepairOrder order) {
        if (isStatus(order, OrderConstants.STATUS_PENDING_DISPATCH)) {
            return;
        }
        throw new BusinessException(ResultCode.CONFLICT, pendingDispatchFailReason(order));
    }

    /** 生成「不可派单」的失败原因文案，供单条派单与批量派单复用 */
    private String pendingDispatchFailReason(RepairOrder order) {
        if (isStatus(order, OrderConstants.STATUS_PENDING_AUDIT)) {
            return DispatchConstants.MSG_NOT_AUDITED_YET;
        }
        return DispatchConstants.MSG_STATUS_NOT_DISPATCHABLE;
    }

    /**
     * 校验目标维修人员（BR-3-03）：必须存在、{@code role = repair}、{@code status = 1}。
     *
     * @return 校验通过的维修人员实体
     */
    private SysUser requireEnabledRepairWorker(Long workerId) {
        if (workerId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, DispatchConstants.MSG_WORKER_REQUIRED);
        }
        SysUser worker = sysUserMapper.selectById(workerId);
        // 用户不存在，或角色不是维修人员，统一按 404「维修人员不存在」处理（接口文档 4.4.2）
        if (worker == null || !RoleEnum.REPAIR.getCode().equalsIgnoreCase(worker.getRole())) {
            throw new BusinessException(ResultCode.NOT_FOUND, DispatchConstants.MSG_WORKER_NOT_FOUND);
        }
        if (worker.getStatus() == null || worker.getStatus() != UserConstants.STATUS_ENABLED) {
            throw new BusinessException(ResultCode.CONFLICT, DispatchConstants.MSG_WORKER_DISABLED);
        }
        return worker;
    }

    /** 状态码比较的安全封装，避免 Integer 拆箱空指针 */
    private boolean isStatus(RepairOrder order, int expected) {
        return order != null && order.getStatus() != null && order.getStatus() == expected;
    }

    // ==================================================================================
    // 十、私有方法 —— 抢单池判定（基于派单审计表推导）
    // ==================================================================================

    /**
     * 判断工单当前是否「在抢单池中」。
     *
     * <p>判定规则（因 {@code repair_order} 无专门字段，依据接口文档 Q3 由审计表推导）：</p>
     * <ol>
     *   <li>历史上存在 {@code dispatch_type = 3}（发布抢单池）的记录；<b>且</b></li>
     *   <li>不存在 {@code dispatch_type ∈ (1 派单, 2 改派, 4 抢单)} 的记录
     *       —— 一旦被派单 / 改派 / 抢单，即视为已出池。</li>
     * </ol>
     *
     * <p>由此自然满足两条业务规则：重复发布返回 409「该工单已在抢单池中」；
     * 工单被领走后自动从 D-06 列表中消失。</p>
     */
    private boolean isInGrabPool(Long orderId) {
        Long published = repairDispatchLogMapper.selectCount(
                new LambdaQueryWrapper<RepairDispatchLog>()
                        .eq(RepairDispatchLog::getOrderId, orderId)
                        .eq(RepairDispatchLog::getDispatchType, DispatchTypeEnum.PUBLISH_GRAB.getCode()));
        if (published == null || published == 0L) {
            return false;
        }
        Long consumed = repairDispatchLogMapper.selectCount(
                new LambdaQueryWrapper<RepairDispatchLog>()
                        .eq(RepairDispatchLog::getOrderId, orderId)
                        .in(RepairDispatchLog::getDispatchType,
                                DispatchTypeEnum.DISPATCH.getCode(),
                                DispatchTypeEnum.REASSIGN.getCode(),
                                DispatchTypeEnum.GRAB.getCode()));
        return consumed == null || consumed == 0L;
    }

    // ==================================================================================
    // 十一、私有方法 —— 审计记录与进度记录写入
    // ==================================================================================

    /**
     * 写入一条派单审计记录（BR-3-08）。
     *
     * <p>统一出口的好处：{@code opResult} 恒为「1 成功」（只有成功的动作才留痕）、
     * {@code dispatchTime} 与业务动作使用<b>同一个时间戳</b>，
     * 保证 D-10 还原出的轨迹时间与工单快照时间严格一致。</p>
     *
     * @param orderId      工单 ID
     * @param type         操作类型枚举
     * @param operatorId   操作人 ID
     * @param fromWorkerId 原受派维修人员 ID（仅改派 / 拒单场景非空）
     * @param toWorkerId   本次操作涉及的维修人员 ID（发布抢单池时为 null）
     * @param time         操作发生时间
     * @param remark       备注（派单/改派/发布说明、拒单原因）
     */
    private void insertDispatchLog(Long orderId, DispatchTypeEnum type, Long operatorId,
                                   Long fromWorkerId, Long toWorkerId,
                                   LocalDateTime time, String remark) {
        RepairDispatchLog dispatchLog = new RepairDispatchLog();
        dispatchLog.setOrderId(orderId);
        dispatchLog.setDispatchType(type.getCode());
        dispatchLog.setOperatorId(operatorId);
        dispatchLog.setFromWorkerId(fromWorkerId);
        dispatchLog.setToWorkerId(toWorkerId);
        dispatchLog.setOpResult(DispatchOpResultEnum.SUCCESS.getCode());
        dispatchLog.setRemark(remark);
        dispatchLog.setDispatchTime(time);
        dispatchLog.setDeleted(0);
        repairDispatchLogMapper.insert(dispatchLog);
    }

    /**
     * 写入一条维修记录（进度时间线节点）。
     *
     * <p>本模块只在两处写入：D-08 接单（{@code record_type = 1}）
     * 与 D-09 拒单（{@code record_type = 6}）；其余类型由模块四写入。</p>
     *
     * @return 新生成的 {@code repair_record.record_id}
     */
    private Long insertRecord(Long orderId, Long workerId, int recordType,
                              String progressContent, LocalDateTime recordTime) {
        RepairRecord record = new RepairRecord();
        record.setOrderId(orderId);
        record.setWorkerId(workerId);
        record.setRecordType(recordType);
        record.setProgressContent(progressContent);
        // 本模块写入的两类节点不涉及配件与费用，按建表口径落 0.00
        record.setCost(BigDecimal.ZERO);
        record.setRecordTime(recordTime);
        record.setDeleted(0);
        repairRecordMapper.insert(record);
        return record.getRecordId();
    }

    // ==================================================================================
    // 十二、私有方法 —— 字段规范化（第二层业务校验）
    //     说明：@Valid 已做第一层校验，这里做第二层（业务语义更精确、可处理纯空格）
    // ==================================================================================

    /** 审核驳回原因规范化：必填 + 5~200 字（BR-3-09） */
    private String normalizeRejectReason(String rejectReason) {
        String value = rejectReason == null ? null : rejectReason.trim();
        if (!StringUtils.hasText(value)
                || value.length() < DispatchConstants.REJECT_REASON_MIN_LENGTH
                || value.length() > DispatchConstants.REJECT_REASON_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    DispatchConstants.MSG_AUDIT_REJECT_REASON_INVALID);
        }
        return value;
    }

    /** 拒单原因规范化：必填 + 5~200 字（BR-3-05） */
    private String normalizeRejectOrderReason(String rejectReason) {
        String value = rejectReason == null ? null : rejectReason.trim();
        if (!StringUtils.hasText(value)
                || value.length() < DispatchConstants.REJECT_ORDER_REASON_MIN_LENGTH
                || value.length() > DispatchConstants.REJECT_ORDER_REASON_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    DispatchConstants.MSG_REJECT_ORDER_REASON_INVALID);
        }
        return value;
    }

    /**
     * 可选备注规范化：去首尾空格；空白串统一落库为 {@code null}，
     * 避免出现「空字符串 vs NULL」两种语义并存造成的展示歧义。
     */
    private String normalizeRemark(String remark, String fieldLabel) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        String value = remark.trim();
        if (value.length() > DispatchConstants.DISPATCH_REMARK_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    fieldLabel + "长度不能超过 255 个字符");
        }
        return value;
    }

    // ==================================================================================
    // 十三、私有方法 —— 关联名称批量装配（避免 N+1）
    // ==================================================================================

    /**
     * 一次性取出工单涉及的类目名称映射。
     * <p>与模块二同一策略：{@code repair_category} 是基础字典表（初始化仅 9 条，
     * 规模恒定），一次全量取出即可构建「类目ID -> 展示名（父类目 / 子类目）」，
     * 优于按 parentId 逐条二次查询。</p>
     */
    private Map<Long, String> loadCategoryNameMap() {
        List<RepairCategory> categories = repairCategoryMapper.selectList(null);
        Map<Long, RepairCategory> categoryMap = new LinkedHashMap<>();
        for (RepairCategory category : categories) {
            categoryMap.put(category.getCategoryId(), category);
        }
        Map<Long, String> nameMap = new LinkedHashMap<>();
        for (RepairCategory category : categories) {
            Long parentId = category.getParentId();
            if (parentId != null && parentId != CategoryConstants.ROOT_PARENT_ID) {
                RepairCategory parent = categoryMap.get(parentId);
                if (parent != null) {
                    nameMap.put(category.getCategoryId(),
                            parent.getCategoryName() + " / " + category.getCategoryName());
                    continue;
                }
            }
            nameMap.put(category.getCategoryId(), category.getCategoryName());
        }
        return nameMap;
    }

    /** 批量装载用户实体（含 role / realName），供派单记录与操作人信息装配使用 */
    private Map<Long, SysUser> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, SysUser> map = new HashMap<>();
        for (SysUser user : users) {
            map.put(user.getUserId(), user);
        }
        return map;
    }

    /** 从用户映射中取姓名；ID 为空或用户已不存在时返回 null */
    private String nameOf(Map<Long, SysUser> userMap, Long userId) {
        SysUser user = userId == null ? null : userMap.get(userId);
        return user == null ? null : user.getRealName();
    }

    /** 把非空 ID 收集进集合，避免把 null 传给 selectBatchIds 造成空指针 */
    private void addIfNotNull(Set<Long> target, Long id) {
        if (id != null) {
            target.add(id);
        }
    }
}
