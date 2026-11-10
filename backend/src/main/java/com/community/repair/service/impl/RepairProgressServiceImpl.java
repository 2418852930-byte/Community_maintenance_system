package com.community.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.community.repair.common.BusinessException;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.OrderConstants;
import com.community.repair.constant.ProgressConstants;
import com.community.repair.convert.ProgressConverter;
import com.community.repair.dto.OrderFinishDTO;
import com.community.repair.dto.OrderRecordCreateDTO;
import com.community.repair.dto.OrderStartDTO;
import com.community.repair.entity.RepairOrder;
import com.community.repair.entity.RepairRecord;
import com.community.repair.entity.SysUser;
import com.community.repair.enums.RecordTypeEnum;
import com.community.repair.enums.RoleEnum;
import com.community.repair.mapper.RepairOrderMapper;
import com.community.repair.mapper.RepairRecordMapper;
import com.community.repair.mapper.SysUserMapper;
import com.community.repair.security.LoginUser;
import com.community.repair.security.UserContext;
import com.community.repair.service.RepairProgressService;
import com.community.repair.vo.OrderDurationVO;
import com.community.repair.vo.OrderFinishVO;
import com.community.repair.vo.OrderRecordCreateVO;
import com.community.repair.vo.OrderRecordVO;
import com.community.repair.vo.OrderStartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 维修进度管理模块 业务实现（模块四）。
 *
 * <p>本类集中承载需求文档 5.4 节 FR-4-01 ~ FR-4-08 与 BR-4-01 ~ BR-4-06 的全部业务规则，
 * 每个方法上方的注释都标注了对应规则编号，便于论文与答辩溯源。</p>
 *
 * <p><b>复用的基础组件（不重复创建）</b>：</p>
 * <ul>
 *   <li>统一响应体 {@code Result} / 状态码字典 {@code ResultCode} + 全局异常处理器
 *       {@code GlobalExceptionHandler}；</li>
 *   <li>鉴权与角色校验 {@code JwtInterceptor} / {@code @RequireRole} / {@code UserContext}
 *       —— <b>操作人身份一律通过 {@link UserContext#require()} 读取，不接受前端传入</b>；</li>
 *   <li>状态字典 {@code OrderStatusEnum} 与常量 {@code OrderConstants}
 *       —— <b>本模块不重复定义工单状态</b>；</li>
 *   <li>实体与 Mapper：{@code RepairRecord} / {@code RepairRecordMapper} 由模块三建立
 *       （D-08 接单、D-09 拒单必须写入本表），<b>本模块直接复用，不重复定义</b>；</li>
 *   <li>字段自动填充 {@code MyMetaObjectHandler}。</li>
 * </ul>
 *
 * <p><b>并发控制与「禁止跳级」（BR-4-02 / NFR-R-02）</b>：所有状态流转都采用
 * 「条件更新 + 受影响行数校验」的乐观锁写法
 * （{@code UPDATE ... WHERE order_id = ? AND status = ?}），
 * 受影响行数不为 1 即判定为并发冲突或状态不合法，统一返回 409。
 * 前置状态被固化在 WHERE 中，任何不在状态机内的迁移都不可能命中。</p>
 *
 * <p><b>数据不可篡改（BR-4-05）</b>：本模块对 {@code repair_record} <b>只做 insert 与 select</b>，
 * 不存在任何 update / delete 路径，从而保证维修记录一经写入即不可修改、不可删除，
 * 维修过程天然可追溯。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepairProgressServiceImpl implements RepairProgressService {

    /** 工单主表：状态流转与费用累加的写入对象（模块二既有 Mapper，本模块只调用不修改） */
    private final RepairOrderMapper repairOrderMapper;

    /** 维修记录表：进度时间轴的数据来源（模块三既有实体与 Mapper，本模块直接复用） */
    private final RepairRecordMapper repairRecordMapper;

    /** 用户表：操作人姓名批量装配（模块零既有 Mapper） */
    private final SysUserMapper sysUserMapper;

    // ==================================================================================
    // 一、R-01 进度时间线查询
    // ==================================================================================

    /**
     * R-01 进度时间线查询。
     *
     * <p>对应需求 FR-4-07、BR-4-05；接口文档 4.5.1。</p>
     *
     * <p>按 {@code record_time} 升序（同一时间再按主键升序）返回工单的全部进度节点。
     * 记录类型可选用 {@code recordType} 筛选，取值域为 1~6。</p>
     *
     * <p><b>数据范围（服务端强制，前端无法绕过）</b>：居民仅可查看本人提交的工单、
     * 维修人员仅可查看本人受派的工单、管理员可查看全部；不符时返回 403
     * 且不泄露工单以外的任何信息。</p>
     *
     * <p><b>无记录时返回空数组而非 404</b>：待审核(8) / 已驳回(9) 工单尚未进入维修链路，
     * 接口文档 4.5.1 明确此时返回空数组，这样前端时间轴组件无需区分「空」与「错」两种状态。</p>
     */
    @Override
    public List<OrderRecordVO> listRecords(Long orderId, Integer recordType) {
        LoginUser current = UserContext.require();

        // 记录类型筛选取值域校验：仅 1~6（数据库 repair_record.record_type 的全部取值）
        if (recordType != null
                && (recordType < ProgressConstants.RECORD_TYPE_MIN
                || recordType > ProgressConstants.RECORD_TYPE_MAX)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, ProgressConstants.MSG_RECORD_TYPE_FILTER_INVALID);
        }

        RepairOrder order = selectExistingOrder(orderId);
        // BR-4-01 的同源规则：查看同样受数据范围约束（居民本人 / 维修人员本人受派 / 管理员全部）
        assertViewPermission(order, current);

        List<RepairRecord> records = repairRecordMapper.selectList(
                new LambdaQueryWrapper<RepairRecord>()
                        .eq(RepairRecord::getOrderId, orderId)
                        .eq(recordType != null, RepairRecord::getRecordType, recordType)
                        .orderByAsc(RepairRecord::getRecordTime)
                        .orderByAsc(RepairRecord::getRecordId));
        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        // 一次性收集操作人 ID 并批量装姓名，避免逐条查询造成 N+1
        Set<Long> workerIds = new LinkedHashSet<>();
        for (RepairRecord item : records) {
            addIfNotNull(workerIds, item.getWorkerId());
        }
        Map<Long, SysUser> userMap = loadUserMap(workerIds);

        List<OrderRecordVO> result = new ArrayList<>(records.size());
        for (RepairRecord item : records) {
            result.add(ProgressConverter.toRecordVO(item, nameOf(userMap, item.getWorkerId())));
        }
        return result;
    }

    // ==================================================================================
    // 二、R-02 追加维修进度
    // ==================================================================================

    /**
     * R-02 追加维修进度。
     *
     * <p>对应需求 FR-4-04、FR-4-05、BR-4-01、BR-4-02、BR-4-03、BR-4-05；接口文档 4.5.2。</p>
     *
     * <p><b>完整校验链（按接口文档 4.5.2 状态码表逐条落地）</b>：</p>
     * <ol>
     *   <li>BR-4-01：工单必须指派给当前登录的维修人员，否则 <b>403</b>；</li>
     *   <li>工单必须存在，否则 <b>404</b>；</li>
     *   <li>BR-4-02 / BR-4-03：状态必须为 <b>2（已接单）或 3（维修中）</b>；
     *       状态为 4（待验收）/ 5（已验收）及 8 / 9 时返回 <b>409</b>
     *       「工单已提交验收，无法继续更新进度」；</li>
     *   <li>记录类型必须为 2 / 3 / 4（1 接单、5 完工、6 转派退回由专有接口写入，禁止伪造）；</li>
     *   <li>进度说明必填 2~200 字；照片 ≤ 3 张；配件 ≤ 255 字；费用非负（均 <b>400</b>）。</li>
     * </ol>
     *
     * <p><b>为什么「暂停 / 等待配件」不新增状态？</b>按接口文档 Q5 的统一口径，
     * 这类中间态以「维修进度(3)」+ 进度说明文本承载，工单状态保持 3（维修中）不变，
     * 因此本接口<b>只追加记录、不迁移状态</b>，严格不越出需求范围。</p>
     *
     * <p><b>费用累加</b>：本次费用通过参数绑定的 {@code set(...)} 显式写入
     * {@code total_cost}（非字符串拼接，从机制上杜绝注入），
     * 同时以 {@code status IN (2,3)} 作为并发兜底条件，保证「状态已被并发推进」时本接口不会误写。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderRecordCreateVO appendRecord(Long orderId, OrderRecordCreateDTO dto) {
        LoginUser current = requireRepair();

        RepairOrder order = selectExistingOrder(orderId);
        // BR-4-01：仅本人受派工单可操作
        assertAssignedWorker(order, current);
        // BR-4-02 / BR-4-03：状态域严格为 2 或 3
        assertAppendableStatus(order);

        // 记录类型取值域：仅 2 到达现场 / 3 维修进度 / 4 更换配件
        if (!RecordTypeEnum.isAppendable(dto.getRecordType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, ProgressConstants.MSG_RECORD_TYPE_INVALID);
        }

        String content = normalizeProgressContent(dto.getProgressContent());
        String images = normalizeImages(dto.getImages());
        String partsUsed = normalizePartsUsed(dto.getPartsUsed());
        BigDecimal cost = normalizeCost(dto.getCost());
        // recordTime 可选，缺省取当前时间；允许补录稍早时间（如现场无网络事后补登）
        LocalDateTime recordTime = dto.getRecordTime() == null ? LocalDateTime.now() : dto.getRecordTime();

        // BR-4-05：写入即留痕，本模块不提供任何修改 / 删除路径
        Long recordId = insertRecord(orderId, current.getUserId(), dto.getRecordType().intValue(),
                content, images, partsUsed, cost, recordTime);

        // 本次费用自动累加至工单 total_cost；条件中含状态域，作为并发兜底
        BigDecimal oldTotal = order.getTotalCost() == null ? BigDecimal.ZERO : order.getTotalCost();
        BigDecimal newTotal = oldTotal.add(cost).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .in(RepairOrder::getStatus, toStatusList(ProgressConstants.APPENDABLE_STATUSES))
                .set(RepairOrder::getTotalCost, newTotal)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, wrapper) != 1) {
            // 并发场景：本请求处理期间工单已被提交验收（3 → 4）
            throw new BusinessException(ResultCode.CONFLICT, ProgressConstants.MSG_STATUS_NOT_APPENDABLE);
        }

        order.setTotalCost(newTotal);
        order.setUpdateTime(now);

        RepairRecord saved = new RepairRecord();
        saved.setRecordId(recordId);
        saved.setOrderId(orderId);
        saved.setRecordType(dto.getRecordType());
        saved.setRecordTime(recordTime);

        log.info("维修人员 {} 追加维修进度：orderId={}, recordId={}, recordType={}, 累计费用={}",
                current.getUserId(), orderId, recordId, dto.getRecordType(), newTotal);
        return ProgressConverter.toRecordCreateVO(saved, order);
    }

    // ==================================================================================
    // 三、R-03 开始维修
    // ==================================================================================

    /**
     * R-03 开始维修。
     *
     * <p>对应需求 FR-4-03、BR-4-02；接口文档 4.5.3。</p>
     *
     * <p><b>状态迁移 2 → 3，且严格只允许从 2 出发</b>：这是「禁止跳级」最关键的一处落地——
     * 「已派单(1)」必须先经模块三 D-08 接单（1 → 2）才能开始维修，
     * 否则返回 <b>409「请按顺序更新进度」</b>，与接口文档 4.5.3 的状态码说明逐字一致。</p>
     *
     * <p>成功后自动生成一条类型为「到达现场」的记录（{@code record_type = 2}），
     * 该记录时间同时成为 BR-7-02「维修时长」的起点
     * （维修时长 = 完工时间 − 首条「到达现场」记录的记录时间）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderStartVO startRepair(Long orderId, OrderStartDTO dto) {
        LoginUser current = requireRepair();

        RepairOrder order = selectExistingOrder(orderId);
        assertAssignedWorker(order, current);

        // BR-4-02：严格按顺序流转，仅状态 2（已接单）可开始维修
        if (!isStatus(order, OrderConstants.STATUS_ACCEPTED)) {
            throw new BusinessException(ResultCode.CONFLICT, ProgressConstants.MSG_STATUS_NOT_STARTABLE);
        }

        String content = normalizeArriveRemark(dto.getRemark());
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_ACCEPTED)
                .set(RepairOrder::getStatus, OrderConstants.STATUS_REPAIRING)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, wrapper) != 1) {
            // 并发：另一路径已推进状态机
            throw new BusinessException(ResultCode.CONFLICT, ProgressConstants.MSG_STATUS_NOT_STARTABLE);
        }

        Long recordId = insertRecord(orderId, current.getUserId(), RecordTypeEnum.ARRIVE.getCode(),
                content, null, null, BigDecimal.ZERO, now);

        order.setStatus(OrderConstants.STATUS_REPAIRING);
        order.setUpdateTime(now);

        log.info("维修人员 {} 开始维修：orderId={}, 状态 2 → 3, recordId={}",
                current.getUserId(), orderId, recordId);
        return ProgressConverter.toStartVO(order, recordId, now);
    }

    // ==================================================================================
    // 四、R-04 提交维修完成
    // ==================================================================================

    /**
     * R-04 提交维修完成。
     *
     * <p>对应需求 FR-4-06、BR-4-02、BR-4-04、BR-4-06；接口文档 4.5.4。</p>
     *
     * <p><b>状态迁移 3 → 4</b>，成功写入 {@code finish_time}，并生成一条「完工」记录
     * （{@code record_type = 5}）。BR-4-04 要求维修结果说明<b>必填且 10~300 字</b>，
     * 这是「完工节点不能是一句空话」的质量门禁。</p>
     *
     * <p><b>BR-4-06 返工后可再次提交</b>：居民验收驳回后工单由模块五回退至 3（维修中），
     * 此时可再次调用本接口。因此本接口<b>不限制调用次数</b>，
     * 只以「当前状态是否为 3」作为唯一前置条件，天然支持多轮「驳回 → 重修 → 再提交」。</p>
     *
     * <p><b>本接口是模块四流向模块五的唯一出口</b>：落库后 {@code repair_order} 具备
     * {@code status = 4}、{@code finish_time}、{@code total_cost}，
     * 加上 {@code repair_record} 中的完工说明，即构成下游「待验收工单快捷查询（V-01）」
     * 与「验收（V-02）」所需的完整输入，<b>无需任何数据结构适配</b>。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderFinishVO finishRepair(Long orderId, OrderFinishDTO dto) {
        LoginUser current = requireRepair();

        RepairOrder order = selectExistingOrder(orderId);
        assertAssignedWorker(order, current);

        // BR-4-02：严格按顺序流转，仅状态 3（维修中）可提交完成
        if (!isStatus(order, OrderConstants.STATUS_REPAIRING)) {
            throw new BusinessException(ResultCode.CONFLICT, ProgressConstants.MSG_STATUS_NOT_FINISHABLE);
        }

        // BR-4-04：维修结果说明必填，10~300 字
        String content = normalizeFinishContent(dto.getProgressContent());
        String images = normalizeImages(dto.getImages());
        String partsUsed = normalizePartsUsed(dto.getPartsUsed());
        BigDecimal cost = normalizeCost(dto.getCost());

        BigDecimal oldTotal = order.getTotalCost() == null ? BigDecimal.ZERO : order.getTotalCost();
        BigDecimal newTotal = oldTotal.add(cost).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_REPAIRING)
                .set(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_VERIFY)
                .set(RepairOrder::getFinishTime, now)
                .set(RepairOrder::getTotalCost, newTotal)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, wrapper) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, ProgressConstants.MSG_STATUS_NOT_FINISHABLE);
        }

        Long recordId = insertRecord(orderId, current.getUserId(), RecordTypeEnum.FINISH.getCode(),
                content, images, partsUsed, cost, now);

        order.setStatus(OrderConstants.STATUS_PENDING_VERIFY);
        order.setFinishTime(now);
        order.setTotalCost(newTotal);
        order.setUpdateTime(now);

        log.info("维修人员 {} 提交维修完成：orderId={}, 状态 3 → 4, finishTime={}, 累计费用={}, recordId={}",
                current.getUserId(), orderId, now, newTotal, recordId);
        return ProgressConverter.toFinishVO(order, recordId);
    }

    // ==================================================================================
    // 五、R-05 维修耗时统计
    // ==================================================================================

    /**
     * R-05 维修耗时统计。
     *
     * <p>对应需求 FR-4-08、BR-7-02（接口文档 Q6 已统一口径）；接口文档 4.5.5。</p>
     *
     * <p><b>计算口径</b>：各时间点全部取 {@code repair_order} 上的快照字段，
     * 唯一的例外是「开始维修时间」—— 它需要查 {@code repair_record} 中该工单
     * <b>首条 {@code record_type = 2}（到达现场）</b> 记录的 {@code record_time}，
     * 因为工单表没有专门的「开始维修时间」列（这是 BR-7-02 与本表结构的既定对应关系）。</p>
     *
     * <p>未流转到的节点其时间为 {@code null}，对应时长同样为 {@code null}，
     * 与接口文档「未流转到的节点时间为 null，对应时长也为 null」一致；
     * 待审核(8) / 已驳回(9) 工单尚未派单，因此各耗时项均为 {@code null}。</p>
     *
     * <p><b>数据范围</b>：管理员可查任意工单；维修人员仅可查本人受派工单，否则返回 403。</p>
     */
    @Override
    public OrderDurationVO getOrderDuration(Long orderId) {
        LoginUser current = UserContext.require();

        RepairOrder order = selectExistingOrder(orderId);
        boolean isAdmin = RoleEnum.ADMIN.getCode().equalsIgnoreCase(current.getRole());
        if (!isAdmin) {
            // 仅维修人员可查看本人受派工单，其余角色（含居民）一律 403
            boolean isAssignedWorker = RoleEnum.REPAIR.getCode().equalsIgnoreCase(current.getRole())
                    && Objects.equals(order.getWorkerId(), current.getUserId());
            if (!isAssignedWorker) {
                log.warn("越权查看维修耗时被拦截：userId={}, role={}, orderId={}",
                        current.getUserId(), current.getRole(), orderId);
                throw new BusinessException(ResultCode.FORBIDDEN, ProgressConstants.MSG_NO_PERMISSION_ACCESS);
            }
        }

        LocalDateTime startTime = selectRepairStartTime(orderId);
        return ProgressConverter.toDurationVO(order, startTime);
    }

    // ==================================================================================
    // 六、私有方法 —— 身份与权限
    // ==================================================================================

    /**
     * 取得当前登录维修人员身份。
     * <p>兜底防御：{@code @RequireRole(REPAIR)} 已在拦截器层拦截非维修人员，
     * 此处再校验一次，确保 Service 被其它入口调用时依然安全（NFR-S-03）。</p>
     */
    private LoginUser requireRepair() {
        LoginUser current = UserContext.require();
        if (!RoleEnum.REPAIR.getCode().equalsIgnoreCase(current.getRole())) {
            log.warn("非维修人员越权调用维修进度接口被拦截：userId={}, role={}",
                    current.getUserId(), current.getRole());
            throw new BusinessException(ResultCode.FORBIDDEN, ProgressConstants.MSG_ROLE_NOT_REPAIR);
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

    /**
     * 查询视角的数据范围校验（R-01、R-05 共用）。
     *
     * <p>居民仅本人提交、维修人员仅本人受派、管理员不限；其余组合一律 403。</p>
     */
    private void assertViewPermission(RepairOrder order, LoginUser current) {
        String role = current.getRole();
        Long userId = current.getUserId();
        if (RoleEnum.ADMIN.getCode().equalsIgnoreCase(role)) {
            return;
        }
        if (RoleEnum.REPAIR.getCode().equalsIgnoreCase(role)) {
            if (Objects.equals(order.getWorkerId(), userId)) {
                return;
            }
        } else if (RoleEnum.RESIDENT.getCode().equalsIgnoreCase(role)) {
            if (Objects.equals(order.getResidentId(), userId)) {
                return;
            }
        }
        log.warn("越权查看维修进度被拦截：userId={}, role={}, orderId={}",
                userId, role, order.getOrderId());
        throw new BusinessException(ResultCode.FORBIDDEN, OrderConstants.MSG_NO_PERMISSION_VIEW);
    }

    // ==================================================================================
    // 七、私有方法 —— 状态与目标对象校验
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
     * 追加进度的状态前置断言（BR-4-02 / BR-4-03）。
     * <p>状态域为 2（已接单）/ 3（维修中）；4（待验收）及其后一律拒绝。</p>
     */
    private void assertAppendableStatus(RepairOrder order) {
        if (order.getStatus() == null) {
            throw new BusinessException(ResultCode.CONFLICT, ProgressConstants.MSG_STATUS_NOT_APPENDABLE);
        }
        for (int allowed : ProgressConstants.APPENDABLE_STATUSES) {
            if (order.getStatus() == allowed) {
                return;
            }
        }
        throw new BusinessException(ResultCode.CONFLICT, ProgressConstants.MSG_STATUS_NOT_APPENDABLE);
    }

    /** 状态码比较的安全封装，避免 Integer 拆箱空指针 */
    private boolean isStatus(RepairOrder order, int expected) {
        return order != null && order.getStatus() != null && order.getStatus() == expected;
    }

    /**
     * 查询该工单的「开始维修时间」= 首条「到达现场」记录（{@code record_type = 2}）的记录时间。
     *
     * <p>按 {@code record_time} 升序取第一条：即使未来出现多条「到达现场」记录
     * （例如验收驳回后再次到场），BR-7-02 的维修时长仍以<b>首次</b>到场为起点。</p>
     *
     * @return 开始维修时间；尚未产生「到达现场」记录时返回 null
     */
    private LocalDateTime selectRepairStartTime(Long orderId) {
        RepairRecord first = repairRecordMapper.selectOne(
                new LambdaQueryWrapper<RepairRecord>()
                        .eq(RepairRecord::getOrderId, orderId)
                        .eq(RepairRecord::getRecordType, RecordTypeEnum.ARRIVE.getCode())
                        .orderByAsc(RepairRecord::getRecordTime)
                        .orderByAsc(RepairRecord::getRecordId)
                        // 固定字面量，不含任何用户输入，不存在注入风险
                        .last("LIMIT 1"));
        return first == null ? null : first.getRecordTime();
    }

    // ==================================================================================
    // 八、私有方法 —— 维修记录写入
    // ==================================================================================

    /**
     * 写入一条维修记录（进度时间线节点）。
     *
     * <p>全模块唯一的 {@code repair_record} 写入出口：类型、内容、图片、配件、费用、
     * 业务时间与操作人一次写全，且 {@code create_time} / {@code update_time}
     * 交由 {@code MyMetaObjectHandler} 自动填充（方案 A 的时间字段配合要求）。</p>
     *
     * @return 新生成的 {@code repair_record.record_id}
     */
    private Long insertRecord(Long orderId, Long workerId, int recordType, String progressContent,
                              String images, String partsUsed, BigDecimal cost, LocalDateTime recordTime) {
        RepairRecord record = new RepairRecord();
        record.setOrderId(orderId);
        record.setWorkerId(workerId);
        record.setRecordType(recordType);
        record.setProgressContent(progressContent);
        record.setImages(images);
        record.setPartsUsed(partsUsed);
        // 费用列 NOT NULL DEFAULT 0.00，未传时按建表口径落 0.00
        record.setCost(cost == null ? BigDecimal.ZERO : cost);
        record.setRecordTime(recordTime);
        record.setDeleted(0);
        repairRecordMapper.insert(record);
        return record.getRecordId();
    }

    // ==================================================================================
    // 九、私有方法 —— 字段规范化（第二层业务校验）
    //     说明：@Valid 已做第一层校验，这里做第二层（业务语义更精确、可处理纯空格）
    // ==================================================================================

    /** 进度说明规范化：必填 + 2~200 字（FR-4-04） */
    private String normalizeProgressContent(String progressContent) {
        String value = progressContent == null ? null : progressContent.trim();
        if (!StringUtils.hasText(value)
                || value.length() < ProgressConstants.PROGRESS_CONTENT_MIN_LENGTH
                || value.length() > ProgressConstants.PROGRESS_CONTENT_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, ProgressConstants.MSG_PROGRESS_CONTENT_INVALID);
        }
        return value;
    }

    /** 维修结果说明规范化：必填 + 10~300 字（BR-4-04） */
    private String normalizeFinishContent(String progressContent) {
        String value = progressContent == null ? null : progressContent.trim();
        if (!StringUtils.hasText(value)
                || value.length() < ProgressConstants.FINISH_CONTENT_MIN_LENGTH
                || value.length() > ProgressConstants.FINISH_CONTENT_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, ProgressConstants.MSG_FINISH_CONTENT_INVALID);
        }
        return value;
    }

    /**
     * 到场说明规范化：可选 + ≤ 200 字；空白串统一落库为 {@code null}，
     * 避免出现「空字符串 vs NULL」两种语义并存。
     */
    private String normalizeArriveRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return ProgressConstants.DEFAULT_ARRIVE_CONTENT;
        }
        String value = remark.trim();
        if (value.length() > ProgressConstants.ARRIVE_REMARK_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, ProgressConstants.MSG_ARRIVE_REMARK_TOO_LONG);
        }
        return value;
    }

    /**
     * 图片路径规范化：拆分为路径数组 → 去空白项 → 校验张数 → 以英文逗号重新拼接。
     *
     * <p>重新拼接而非原样落库，是为了让数据库中的逗号分隔格式统一
     * （避免存入「a, b ,c」这类带空格的脏数据，前端 {@code split(',')} 后出现空路径）。</p>
     *
     * @return 规范化后的路径串；无有效路径时返回 null
     */
    private String normalizeImages(String images) {
        if (!StringUtils.hasText(images)) {
            return null;
        }
        Set<String> paths = new LinkedHashSet<>();
        for (String path : images.split(",")) {
            if (StringUtils.hasText(path)) {
                paths.add(path.trim());
            }
        }
        if (paths.isEmpty()) {
            return null;
        }
        // FR-4-05：维修过程照片最多 3 张
        if (paths.size() > ProgressConstants.IMAGES_MAX_COUNT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, ProgressConstants.MSG_IMAGES_TOO_MANY);
        }
        String value = String.join(",", paths);
        if (value.length() > ProgressConstants.IMAGES_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "图片路径总长度不能超过 " + ProgressConstants.IMAGES_MAX_LENGTH + " 个字符");
        }
        return value;
    }

    /** 配件说明规范化：可选 + ≤ 255 字；空白串落库为 null */
    private String normalizePartsUsed(String partsUsed) {
        if (!StringUtils.hasText(partsUsed)) {
            return null;
        }
        String value = partsUsed.trim();
        if (value.length() > ProgressConstants.PARTS_USED_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, ProgressConstants.MSG_PARTS_USED_TOO_LONG);
        }
        return value;
    }

    /**
     * 费用规范化：缺省 {@code 0.00}，不可为负，且不超过列宽上限；
     * 统一四舍五入到 2 位小数（对应 DECIMAL(10,2)）。
     */
    private BigDecimal normalizeCost(BigDecimal cost) {
        if (cost == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (cost.signum() < 0 || cost.compareTo(ProgressConstants.COST_MAX) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, ProgressConstants.MSG_COST_INVALID);
        }
        return cost.setScale(2, RoundingMode.HALF_UP);
    }

    // ==================================================================================
    // 十、私有方法 —— 关联名称批量装配（避免 N+1）
    // ==================================================================================

    /** 批量装载用户实体，供操作人姓名装配使用 */
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

    /**
     * {@code int[]} -> {@code List<Integer>} 的转换，供 {@code wrapper.in(...)} 使用。
     *
     * <p>{@code int[]} 是基本类型数组，既不能直接作为 {@code Collection} 传入，
     * 直接传 {@code Integer[]} 又会命中 {@code in(R column, Object... values)} 的变长重载
     * 并触发 javac 的 varargs 告警。这里显式装箱为 {@code List<Integer>}，
     * 精确命中 {@code in(R column, Collection<?> coll)} 重载，语义清晰且零告警。</p>
     */
    private List<Integer> toStatusList(int[] codes) {
        List<Integer> result = new ArrayList<>(codes.length);
        for (int code : codes) {
            result.add(code);
        }
        return result;
    }
}
