package com.community.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.repair.common.BusinessException;
import com.community.repair.common.PageResult;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.CategoryConstants;
import com.community.repair.constant.OrderConstants;
import com.community.repair.constant.VerifyConstants;
import com.community.repair.convert.OrderConverter;
import com.community.repair.convert.VerifyConverter;
import com.community.repair.dto.AgentVerifyDTO;
import com.community.repair.dto.OrderVerifyDTO;
import com.community.repair.dto.PendingVerifyQueryDTO;
import com.community.repair.entity.RepairCategory;
import com.community.repair.entity.RepairOrder;
import com.community.repair.entity.RepairVerifyLog;
import com.community.repair.entity.SysUser;
import com.community.repair.enums.RoleEnum;
import com.community.repair.enums.VerifyResultEnum;
import com.community.repair.enums.VerifyTypeEnum;
import com.community.repair.mapper.RepairCategoryMapper;
import com.community.repair.mapper.RepairOrderMapper;
import com.community.repair.mapper.RepairVerifyLogMapper;
import com.community.repair.mapper.SysUserMapper;
import com.community.repair.security.LoginUser;
import com.community.repair.security.UserContext;
import com.community.repair.service.RepairVerifyService;
import com.community.repair.vo.AgentVerifyVO;
import com.community.repair.vo.OrderListVO;
import com.community.repair.vo.VerifyRecordVO;
import com.community.repair.vo.VerifyResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 工单验收管理模块 业务实现（模块五）。
 *
 * <p>本类集中承载需求文档 5.5 节 FR-5-01 ~ FR-5-07 与 BR-5-01 ~ BR-5-08 的全部业务规则，
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
 *   <li>列表元素转换 {@code OrderConverter.toListVO} 与超时派生计算
 *       {@code OrderConverter.isVerifyOverdue}
 *       —— <b>V-01 的出参与 O-02 共用同一份口径，不另写一套</b>；</li>
 *   <li>实体与 Mapper：{@code RepairOrder} / {@code RepairOrderMapper} 由模块二建立、
 *       {@code RepairCategory} / {@code SysUser} 由模块零、一建立，本模块直接复用；</li>
 *   <li>字段自动填充 {@code MyMetaObjectHandler}。</li>
 * </ul>
 *
 * <p><b>并发控制与「禁止跳级」（BR-5-01 / NFR-R-02）</b>：V-02 / V-03 的状态流转
 * 一律采用「条件更新 + 受影响行数校验」的乐观锁写法
 * （{@code UPDATE repair_order SET ... WHERE order_id = ? AND status = 4}），
 * 受影响行数不为 1 即判定为并发冲突或状态不合法，统一返回 409。
 * 前置状态被固化在 WHERE 中，任何不在状态机内的迁移（4 → 6、5 → 3 等）都不可能命中。</p>
 *
 * <p><b>数据不可篡改（BR-5-05）</b>：本模块对 {@code repair_verify_log}
 * <b>只做 insert 与 select</b>，不存在任何 update / delete 路径，
 * 验收留痕一经写入即不可修改、不可删除，多轮验收过程天然可追溯。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepairVerifyServiceImpl implements RepairVerifyService {

    /** 工单主表：状态流转与验收快照字段的写入对象（模块二既有 Mapper，本模块只调用不修改） */
    private final RepairOrderMapper repairOrderMapper;

    /** 验收记录表：多轮验收留痕的写入与查询对象（本模块建立） */
    private final RepairVerifyLogMapper repairVerifyLogMapper;

    /** 类目字典：V-01 列表元素的类目名称装配（模块一既有 Mapper） */
    private final RepairCategoryMapper repairCategoryMapper;

    /** 用户表：居民 / 维修人员 / 验收人姓名批量装配（模块零既有 Mapper） */
    private final SysUserMapper sysUserMapper;

    /** 时间格式：与全局 JacksonConfig 保持一致 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(VerifyConstants.TIME_PATTERN);

    /** 日期格式：查询条件允许只传日期，自动补 00:00:00 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(VerifyConstants.DATE_PATTERN);

    // ==================================================================================
    // 一、V-01 待验收工单快捷查询
    // ==================================================================================

    /**
     * V-01 待验收工单快捷查询。
     *
     * <p>对应需求 FR-5-01、FR-5-06；接口文档 4.6.1。</p>
     *
     * <p><b>数据范围（服务端强制，前端无法绕过）</b>：居民强制附加
     * {@code resident_id = 当前用户ID}，即「我的待验收」；管理员不限定；
     * 维修人员调用本接口直接返回 <b>403</b>（接口文档 4.6.1 状态码表明确列出该场景）。</p>
     *
     * <p><b>与 O-02 的分工</b>：O-02 是「一个接口支撑多套视图」的通用列表，
     * 其时间区间筛的是 {@code create_time}（提交时间）；而本接口是验收场景专用，
     * 时间区间筛的是 <b>{@code finish_time}（完工时间）</b>，
     * 因为居民真正关心的是「哪几天修好的我还没验收」。</p>
     *
     * <p><b>排序口径</b>：按 {@code finish_time} <b>升序</b>（最早完工的排最前），
     * 使积压最久、最可能超时的工单自然浮到列表顶部，与 FR-5-06 的超时提醒形成配合；
     * 同一时间再按主键升序，保证分页结果稳定不跳行。</p>
     *
     * <p><b>{@code onlyOverdue} 的 SQL 口径与 Java 口径一致</b>：
     * 阈值统一取 {@code OrderConstants.VERIFY_OVERDUE_HOURS}（48 小时），
     * Java 侧的超时标记由 {@code OrderConverter.isVerifyOverdue} 计算；
     * 由于状态 4 必然伴随 {@code finish_time}（由模块四 R-04 在同一事务内写入），
     * SQL 直接以 {@code finish_time < 截止时刻} 过滤，
     * 不会出现「列表筛出来的与标记标出来的不一致」。</p>
     */
    @Override
    public PageResult<OrderListVO> pagePendingVerify(PendingVerifyQueryDTO query) {
        LoginUser current = UserContext.require();
        String role = current.getRole();
        Long userId = current.getUserId();
        PendingVerifyQueryDTO safeQuery = query == null ? new PendingVerifyQueryDTO() : query;

        // 维修人员不得访问待验收列表（接口文档 4.6.1：403 权限不足）
        if (!isResident(role) && !isAdmin(role)) {
            log.warn("非居民/管理员越权访问待验收列表被拦截：userId={}, role={}", userId, role);
            throw new BusinessException(ResultCode.FORBIDDEN, VerifyConstants.MSG_NO_PERMISSION_ACCESS);
        }

        // 分页参数保护：pageNum >= 1，1 <= pageSize <= 100（接口文档 2.5 节）
        int pageNum = (safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1)
                ? 1 : safeQuery.getPageNum();
        int pageSize = (safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1)
                ? VerifyConstants.DEFAULT_PAGE_SIZE
                : Math.min(safeQuery.getPageSize(), VerifyConstants.MAX_PAGE_SIZE);

        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();

        // ---- ① 接口语义固定：只查「待验收(4)」，前端无法改状态 ----
        wrapper.eq(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_VERIFY);

        // ---- ② 角色数据范围（需求文档 3.3，服务端强制注入） ----
        if (isResident(role)) {
            wrapper.eq(RepairOrder::getResidentId, userId);
        }

        // ---- ③ 通用筛选条件 ----
        wrapper.eq(safeQuery.getCategoryId() != null, RepairOrder::getCategoryId, safeQuery.getCategoryId());

        // 完工时间区间（本接口口径，区别于 O-02 的 create_time）
        LocalDateTime startTime = parseTime(safeQuery.getStartTime(), "完工时间起");
        LocalDateTime endTime = parseTime(safeQuery.getEndTime(), "完工时间止");
        wrapper.ge(startTime != null, RepairOrder::getFinishTime, startTime);
        wrapper.le(endTime != null, RepairOrder::getFinishTime, endTime);

        // 仅看超时未验收（FR-5-06）：完工时间早于「当前时间 − 48 小时」
        if (Boolean.TRUE.equals(safeQuery.getOnlyOverdue())) {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(VerifyConstants.OVERDUE_HOURS);
            wrapper.lt(RepairOrder::getFinishTime, cutoff);
        }

        // ---- ④ 排序：完工时间升序（积压最久的优先验收）+ 主键升序保证分页稳定 ----
        wrapper.orderByAsc(RepairOrder::getFinishTime).orderByAsc(RepairOrder::getOrderId);

        Page<RepairOrder> page = repairOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 批量装配名称，避免逐条查询导致的 N+1（与模块二、模块四做法一致）
        NameContext names = loadNames(page.getRecords());
        return PageResult.of(page, order -> OrderConverter.toListVO(
                order,
                names.categoryName(order.getCategoryId()),
                names.userName(order.getResidentId()),
                names.userName(order.getWorkerId())));
    }

    // ==================================================================================
    // 二、V-02 验收通过 / 驳回
    // ==================================================================================

    /**
     * V-02 验收通过 / 驳回。
     *
     * <p>对应需求 FR-5-03、FR-5-04、BR-5-01 ~ BR-5-06；接口文档 4.6.2。</p>
     *
     * <p><b>完整校验链（按接口文档 4.6.2 状态码表逐条落地）</b>：</p>
     * <ol>
     *   <li>角色兜底：仅居民（403）；</li>
     *   <li>工单必须存在，否则 <b>404</b>；</li>
     *   <li>BR-5-06：<b>仅工单提交者本人</b>可验收，否则 <b>403</b>
     *       「无权限操作该工单，仅工单提交者可进行验收」；</li>
     *   <li>BR-5-01：状态必须为 <b>4（待验收）</b>；状态为 5 / 6 时返回 <b>409</b>
     *       「该工单已完成验收」，其余状态返回 <b>409</b>「该工单当前状态不可验收」；</li>
     *   <li>取值域：{@code verifyResult} 只能是 0 / 1，否则 <b>400</b>；</li>
     *   <li>BR-5-03：{@code verifyResult = 0}（驳回）时 {@code verifyRemark}
     *       <b>必填</b>且 5~200 字，否则 <b>400</b>。</li>
     * </ol>
     *
     * <p><b>状态流转（严格遵循全局定义，禁止跳级）</b>：</p>
     * <ul>
     *   <li>通过：<b>4 → 5</b>，写入 {@code verify_time} / {@code verify_result = 1} /
     *       {@code verify_remark}，解锁评价入口（FR-5-03）；</li>
     *   <li>驳回：<b>4 → 3</b>，写入 {@code verify_result = 0} 与 {@code verify_remark}，
     *       <b>不写 {@code verify_time}</b>（驳回并未使工单进入「已验收」，
     *       若写了会让 BR-7-02 的「验收时长」在返修中的工单上算出虚假值），
     *       <b>不改 {@code worker_id}</b>（BR-5-04：原受派维修人员不变，
     *       返修仍由同一人负责，工单可直接回到模块四的「我的工单」）。</li>
     * </ul>
     *
     * <p><b>本接口是模块五流向模块六的唯一出口</b>：通过后 {@code repair_order}
     * 具备 {@code status = 5}，模块六只需 {@code O-02 + status=5} 即可拿到待评价列表，
     * <b>无需任何数据结构适配</b>。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VerifyResultVO verifyOrder(Long orderId, OrderVerifyDTO dto) {
        LoginUser current = requireResident();

        RepairOrder order = selectExistingOrder(orderId);
        // BR-5-06：仅工单提交者本人可验收（核心要求之「仅工单提交者可操作对应工单」）
        assertOrderOwner(order, current);

        // 取值域兜底：@Min/@Max 已拦一次，这里再校验一次，保证非法结果值绝不进入状态机
        Integer verifyResult = dto.getVerifyResult();
        if (!VerifyResultEnum.isValid(verifyResult)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, VerifyConstants.MSG_VERIFY_RESULT_INVALID);
        }

        // BR-5-01：状态前置断言（配合后续条件更新的受影响行数校验，双重防跳级）
        assertVerifiableStatus(order);

        boolean passed = VerifyResultEnum.isPass(verifyResult);
        // BR-5-03：驳回必填 5~200 字；通过时可选，填写则同样受 5~200 字约束
        String remark = normalizeVerifyRemark(dto.getVerifyRemark(), passed);

        Integer verifyRound = nextVerifyRound(orderId);
        LocalDateTime now = LocalDateTime.now();

        int targetStatus = passed ? VerifyConstants.TARGET_STATUS_PASSED : VerifyConstants.TARGET_STATUS_REJECTED;
        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                // 前置状态固化在 WHERE 中：既保证「禁止跳级」，也提供并发乐观锁
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_VERIFY)
                .set(RepairOrder::getStatus, targetStatus)
                .set(RepairOrder::getVerifyResult, verifyResult)
                .set(RepairOrder::getVerifyRemark, remark)
                .set(RepairOrder::getUpdateTime, now);
        if (passed) {
            // 仅通过时写入验收时间：它同时是 BR-7-02「验收时长」的终点
            wrapper.set(RepairOrder::getVerifyTime, now);
        }
        if (repairOrderMapper.update(null, wrapper) != 1) {
            // 并发场景：本请求处理期间工单状态已被其它请求推进（或已验收）
            throw new BusinessException(ResultCode.CONFLICT, VerifyConstants.MSG_STATUS_NOT_VERIFIABLE);
        }

        // BR-5-05：每次验收（无论通过或驳回）均向 repair_verify_log 写入一行独立记录
        insertVerifyLog(orderId, current.getUserId(), VerifyTypeEnum.RESIDENT.getCode(),
                verifyResult, remark, verifyRound, now);

        order.setStatus(targetStatus);
        order.setVerifyResult(verifyResult);
        order.setVerifyRemark(remark);
        if (passed) {
            order.setVerifyTime(now);
        }

        log.info("居民 {} 完成第 {} 轮验收：orderId={}, result={}, 状态 4 → {}, 累计验收轮次={}",
                current.getUserId(), verifyRound, orderId, verifyResult, targetStatus, verifyRound);
        return VerifyConverter.toVerifyResultVO(order, verifyRound, verifyResult, remark);
    }

    // ==================================================================================
    // 三、V-03 管理员代验收
    // ==================================================================================

    /**
     * V-03 管理员代验收。
     *
     * <p>对应需求 FR-5-07、BR-5-07、BR-5-08；接口文档 4.6.3。</p>
     *
     * <p><b>完整校验链</b>：</p>
     * <ol>
     *   <li>角色兜底：仅系统管理员（403）；</li>
     *   <li>工单必须存在，否则 <b>404</b>；</li>
     *   <li>状态必须为 4（待验收）：5 / 6 返回 409「该工单已完成验收」，其余返回 409「该工单当前状态不可验收」；</li>
     *   <li>BR-5-08：必须<b>已超过</b> {@code VerifyConstants.OVERDUE_HOURS}（48 小时）
     *       待验收超时阈值，否则 <b>409</b>「尚未超过超时阈值，暂不支持管理员代验收」；</li>
     *   <li>BR-5-07：代验收说明必填且 5~200 字，否则 <b>400</b>。</li>
     * </ol>
     *
     * <p><b>超时判定复用 {@code OrderConverter.isVerifyOverdue}</b>，
     * 与 V-01 列表上的 {@code verifyOverdue} 标记<b>完全同源</b>：
     * 前端列表标红的那条工单，就是本接口可以代验收的那条，不会出现口径打架。</p>
     *
     * <p><b>该接口仅支持验收通过，不支持代驳回</b>（接口文档 4.6.3 明确约定），
     * 因此入参里根本没有 {@code verifyResult}，从接口设计上杜绝「管理员替居民驳回」。
     * 落库时 {@code verify_type = 2}（管理员代验收），与居民验收永久区分（BR-5-07）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentVerifyVO agentVerify(Long orderId, AgentVerifyDTO dto) {
        LoginUser current = requireAdmin();

        RepairOrder order = selectExistingOrder(orderId);
        assertVerifiableStatus(order);

        // BR-5-08：未达 48 小时超时阈值不允许代验收（与 V-01 的 verifyOverdue 同一口径）
        if (!OrderConverter.isVerifyOverdue(order)) {
            throw new BusinessException(ResultCode.CONFLICT, VerifyConstants.MSG_OVERDUE_NOT_REACHED);
        }

        // BR-5-07：代验收说明必填 5~200 字
        String remark = normalizeAgentRemark(dto.getVerifyRemark());

        Integer verifyRound = nextVerifyRound(orderId);
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_VERIFY)
                .set(RepairOrder::getStatus, VerifyConstants.TARGET_STATUS_PASSED)
                .set(RepairOrder::getVerifyTime, now)
                .set(RepairOrder::getVerifyResult, VerifyResultEnum.PASS.getCode())
                .set(RepairOrder::getVerifyRemark, remark)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, wrapper) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, VerifyConstants.MSG_STATUS_NOT_VERIFIABLE);
        }

        // BR-5-07：verify_type = 2 标识「管理员代验收」，使代验收在数据层面可追溯
        insertVerifyLog(orderId, current.getUserId(), VerifyTypeEnum.ADMIN_AGENT.getCode(),
                VerifyResultEnum.PASS.getCode(), remark, verifyRound, now);

        order.setStatus(VerifyConstants.TARGET_STATUS_PASSED);
        order.setVerifyTime(now);
        order.setVerifyResult(VerifyResultEnum.PASS.getCode());
        order.setVerifyRemark(remark);

        log.info("管理员 {} 代为验收：orderId={}, 第 {} 轮, 状态 4 → 5",
                current.getUserId(), orderId, verifyRound);
        // operatorName 直接取上下文中的姓名（JwtInterceptor 已按数据库最新数据装配，无额外查询）
        return VerifyConverter.toAgentVerifyVO(order, verifyRound, remark, current.getRealName());
    }

    // ==================================================================================
    // 四、V-04 验收记录查询
    // ==================================================================================

    /**
     * V-04 验收记录查询。
     *
     * <p>对应需求 FR-5-05、BR-5-05、BR-5-07；接口文档 4.6.4。</p>
     *
     * <p>直查 {@code repair_verify_log}，按 {@code verify_round} <b>升序</b>
     * 返回全部轮次。这与 {@code repair_order} 上「最近一次验收快照」的字段
     * 形成互补：快照回答「现在是什么状态」，本接口回答「这一路是怎么走过来的」。</p>
     *
     * <p><b>数据范围（服务端强制）</b>：居民仅本人提交、维修人员仅本人受派、管理员全部；
     * 其余组合一律 403。工单无验收记录时<b>返回空数组而非 404</b>
     * （待验收及之前的工单本就还没有验收记录），这样前端收货记录时间线组件
     * 无需区分「空」与「错」两种状态（与模块四 R-01 的处理保持一致）。</p>
     */
    @Override
    public List<VerifyRecordVO> listVerifyRecords(Long orderId) {
        LoginUser current = UserContext.require();

        RepairOrder order = selectExistingOrder(orderId);
        // 与 O-03 工单详情完全一致的数据范围校验规则
        assertViewPermission(order, current);

        List<RepairVerifyLog> logs = repairVerifyLogMapper.selectList(
                new LambdaQueryWrapper<RepairVerifyLog>()
                        .eq(RepairVerifyLog::getOrderId, orderId)
                        .orderByAsc(RepairVerifyLog::getVerifyRound)
                        .orderByAsc(RepairVerifyLog::getVerifyId));
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        // 一次性批量装配验收人姓名与角色，避免逐条查询造成 N+1
        Set<Long> verifierIds = new LinkedHashSet<>();
        for (RepairVerifyLog item : logs) {
            addIfNotNull(verifierIds, item.getVerifierId());
        }
        Map<Long, SysUser> userMap = loadUserMap(verifierIds);

        List<VerifyRecordVO> result = new ArrayList<>(logs.size());
        for (RepairVerifyLog item : logs) {
            SysUser verifier = item.getVerifierId() == null ? null : userMap.get(item.getVerifierId());
            result.add(VerifyConverter.toVerifyRecordVO(
                    item,
                    verifier == null ? null : verifier.getRealName(),
                    verifier == null ? null : verifier.getRole()));
        }
        return result;
    }

    // ==================================================================================
    // 五、私有方法 —— 身份与权限
    // ==================================================================================

    /**
     * 取得当前登录居民身份。
     * <p>兜底防御：{@code @RequireRole(RESIDENT)} 已在拦截器层拦截非居民，
     * 此处再校验一次，确保 Service 被其它入口调用时依然安全（NFR-S-03）。</p>
     */
    private LoginUser requireResident() {
        LoginUser current = UserContext.require();
        if (!isResident(current.getRole())) {
            log.warn("非居民越权调用验收接口被拦截：userId={}, role={}",
                    current.getUserId(), current.getRole());
            throw new BusinessException(ResultCode.FORBIDDEN, VerifyConstants.MSG_ROLE_NOT_RESIDENT);
        }
        return current;
    }

    /**
     * 取得当前登录管理员身份。
     * <p>兜底防御：{@code @RequireRole(ADMIN)} 已在拦截器层拦截非管理员，此处再校验一次。</p>
     */
    private LoginUser requireAdmin() {
        LoginUser current = UserContext.require();
        if (!isAdmin(current.getRole())) {
            log.warn("非管理员越权调用代验收接口被拦截：userId={}, role={}",
                    current.getUserId(), current.getRole());
            throw new BusinessException(ResultCode.FORBIDDEN, VerifyConstants.MSG_ROLE_NOT_ADMIN);
        }
        return current;
    }

    /**
     * 数据归属校验：工单必须由当前登录居民本人提交（BR-5-06）。
     *
     * <p><b>这是本模块的核心安全边界</b> —— 即使用户通过篡改请求直接调用 V-02，
     * 也无法验收他人的工单。不通过抛 403，且提示语明确说明「仅工单提交者可进行验收」。</p>
     */
    private void assertOrderOwner(RepairOrder order, LoginUser current) {
        if (!Objects.equals(order.getResidentId(), current.getUserId())) {
            log.warn("居民越权验收非本人提交的工单：userId={}, orderId={}, 实际提交人={}",
                    current.getUserId(), order.getOrderId(), order.getResidentId());
            throw new BusinessException(ResultCode.FORBIDDEN, VerifyConstants.MSG_NOT_ORDER_OWNER);
        }
    }

    /**
     * 查询视角的数据范围校验（V-04 使用，规则与模块二 O-03 / 模块四 R-01 完全一致）。
     *
     * <p>居民仅本人提交、维修人员仅本人受派、管理员不限；其余组合一律 403。</p>
     */
    private void assertViewPermission(RepairOrder order, LoginUser current) {
        String role = current.getRole();
        Long userId = current.getUserId();
        if (isAdmin(role)) {
            return;
        }
        if (RoleEnum.REPAIR.getCode().equalsIgnoreCase(role)) {
            if (Objects.equals(order.getWorkerId(), userId)) {
                return;
            }
        } else if (isResident(role)) {
            if (Objects.equals(order.getResidentId(), userId)) {
                return;
            }
        }
        log.warn("越权查看验收记录被拦截：userId={}, role={}, orderId={}",
                userId, role, order.getOrderId());
        throw new BusinessException(ResultCode.FORBIDDEN, VerifyConstants.MSG_NO_PERMISSION_ACCESS);
    }

    // ==================================================================================
    // 六、私有方法 —— 目标对象与状态校验
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
     * 验收的状态前置断言（BR-5-01）。
     *
     * <p>区分两种 409 提示，与接口文档 4.6.2 的状态码表逐条对应：</p>
     * <ul>
     *   <li>状态为 <b>5（已验收）/ 6（已完成）</b> → 「该工单已完成验收」
     *       —— 这是「重复验收」场景，语义上比「状态不合法」更精确；</li>
     *   <li>其余非 4 状态（含 3 维修中、8 待审核、9 已驳回等） → 「该工单当前状态不可验收」。</li>
     * </ul>
     */
    private void assertVerifiableStatus(RepairOrder order) {
        Integer status = order.getStatus();
        if (status == null) {
            throw new BusinessException(ResultCode.CONFLICT, VerifyConstants.MSG_STATUS_NOT_VERIFIABLE);
        }
        for (int allowed : VerifyConstants.VERIFIABLE_STATUSES) {
            if (status == allowed) {
                return;
            }
        }
        // 已是终态（已验收 5 / 已完成 6）：明确提示为「重复验收」
        if (status == OrderConstants.STATUS_VERIFIED || status == OrderConstants.STATUS_FINISHED) {
            throw new BusinessException(ResultCode.CONFLICT, VerifyConstants.MSG_ALREADY_VERIFIED);
        }
        throw new BusinessException(ResultCode.CONFLICT, VerifyConstants.MSG_STATUS_NOT_VERIFIABLE);
    }

    // ==================================================================================
    // 七、私有方法 —— 验收留痕写入
    // ==================================================================================

    /**
     * 计算下一轮验收轮次：{@code MAX(verify_round) + 1}，无记录时从 1 开始。
     *
     * <p>BR-5-05 要求每轮验收独立留痕，轮次是 V-04 排序与前端展示「第 N 轮」的依据。
     * 并发下由 {@code uk_order_round (order_id, verify_round)} 唯一索引兜底，
     * 冲突由 {@link #insertVerifyLog} 捕获并转为 409。</p>
     */
    private Integer nextVerifyRound(Long orderId) {
        Integer maxRound = repairVerifyLogMapper.selectMaxVerifyRound(orderId);
        return maxRound == null ? VerifyConstants.VERIFY_ROUND_INIT : maxRound + 1;
    }

    /**
     * 写入一条验收记录（验收留痕）。
     *
     * <p>全模块唯一的 {@code repair_verify_log} 写入出口：轮次、验收人、类型、结果、
     * 意见、业务时间一次写全，且 {@code create_time} / {@code update_time}
     * 交由 {@code MyMetaObjectHandler} 自动填充（方案 A 的时间字段配合要求）。</p>
     *
     * <p><b>并发兜底</b>：极端情况下两个请求同时算出同一轮次，
     * 数据库唯一索引会抛 {@link DuplicateKeyException}；
     * 这里把它转换为 409，避免把数据完整性冲突暴露成 500（NFR-R-03）。</p>
     */
    private void insertVerifyLog(Long orderId, Long verifierId, int verifyType, int verifyResult,
                                 String verifyRemark, int verifyRound, LocalDateTime verifyTime) {
        RepairVerifyLog logEntity = new RepairVerifyLog();
        logEntity.setOrderId(orderId);
        logEntity.setVerifyRound(verifyRound);
        logEntity.setVerifierId(verifierId);
        logEntity.setVerifyType(verifyType);
        logEntity.setVerifyResult(verifyResult);
        logEntity.setVerifyRemark(verifyRemark);
        logEntity.setVerifyTime(verifyTime);
        logEntity.setDeleted(0);
        try {
            repairVerifyLogMapper.insert(logEntity);
        } catch (DuplicateKeyException e) {
            log.warn("验收留痕写入冲突（轮次重复）：orderId={}, verifyRound={}", orderId, verifyRound);
            throw new BusinessException(ResultCode.CONFLICT, VerifyConstants.MSG_VERIFY_CONFLICT);
        }
    }

    // ==================================================================================
    // 八、私有方法 —— 字段规范化（第二层业务校验）
    //     说明：@Valid 已做第一层校验，这里做第二层（业务语义更精确、可处理纯空格）
    // ==================================================================================

    /**
     * 验收意见规范化。
     *
     * <p>BR-5-03 规定：「驳回时必填，5~200 字」。因此本方法的行为依赖 {@code passed}：</p>
     * <ul>
     *   <li>驳回：必须有值且 5~200 字，否则 400；</li>
     *   <li>通过：可留空（落库为 {@code null}），若填写则同样须 5~200 字
     *       —— 不允许存「两个字」的半截意见，保证数据质量可控。</li>
     * </ul>
     * <p>空白串统一转为 {@code null}，避免「空字符串 vs NULL」两种语义并存；
     * 同时也防住了「用一串空格绕过 @NotBlank」这类入参。</p>
     *
     * @param verifyRemark 原始验收意见
     * @param passed       本次是否为「通过」
     * @return 规范化后的意见（通过且留空时为 null）
     */
    private String normalizeVerifyRemark(String verifyRemark, boolean passed) {
        String value = verifyRemark == null ? null : verifyRemark.trim();
        if (!StringUtils.hasText(value)) {
            if (!passed) {
                throw new BusinessException(ResultCode.BAD_REQUEST, VerifyConstants.MSG_REJECT_REMARK_REQUIRED);
            }
            return null;
        }
        if (value.length() < VerifyConstants.VERIFY_REMARK_MIN_LENGTH
                || value.length() > VerifyConstants.VERIFY_REMARK_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, VerifyConstants.MSG_VERIFY_REMARK_INVALID);
        }
        return value;
    }

    /** 代验收说明规范化：必填 + 5~200 字（BR-5-07） */
    private String normalizeAgentRemark(String verifyRemark) {
        String value = verifyRemark == null ? null : verifyRemark.trim();
        if (!StringUtils.hasText(value)
                || value.length() < VerifyConstants.AGENT_REMARK_MIN_LENGTH
                || value.length() > VerifyConstants.AGENT_REMARK_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, VerifyConstants.MSG_AGENT_REMARK_INVALID);
        }
        return value;
    }

    /**
     * 解析时间查询参数，兼容 {@code yyyy-MM-dd HH:mm:ss} 与 {@code yyyy-MM-dd} 两种写法。
     *
     * @param text       原始字符串
     * @param fieldLabel 字段中文名，用于拼装更精确的 400 提示
     * @return 解析后的时间；参数为空时返回 {@code null}
     */
    private LocalDateTime parseTime(String text, String fieldLabel) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            // 只传日期时按当日 00:00:00 处理，避免前端漏拼时分秒导致 400
            if (value.length() == 10) {
                return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay();
            }
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldLabel + "：" + VerifyConstants.MSG_TIME_FORMAT);
        }
    }

    // ==================================================================================
    // 九、私有方法 —— 关联名称批量装配（避免 N+1）
    // ==================================================================================

    /**
     * 批量装配 V-01 列表元素的关联名称（类目名 / 居民名 / 维修人员名）。
     *
     * <p><b>类目为何查全表？</b>{@code repair_category} 是基础数据字典表（初始化仅 9 条，
     * 规模恒定在个位/十位数），一次全量取出即可同时构建「类目ID -> 类目实体」，
     * 从而在一次查询内完成「子类目 + 父类目名」的拼装，优于按 parentId 二次查询。</p>
     *
     * <p><b>为何不直接复用模块二的 {@code RepairOrderService.pageOrders}？</b>
     * 因为 V-01 的筛选口径（按完工时间、仅超时）与 O-02（按提交时间、支持关键字与多状态）
     * 本就不同，且本接口不允许维修人员访问。共用方法反而要加一堆开关参数。
     * 这里按「同口径复用、异口径独立」的原则，只复用<b>转换口径</b>
     * （{@code OrderConverter.toListVO} 与 {@code isVerifyOverdue}），
     * 名称装配逻辑则保持与模块二一致的实现方式，确保两处列表长得一模一样。</p>
     */
    private NameContext loadNames(List<RepairOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return new NameContext(new HashMap<>(), new HashMap<>());
        }

        // ---- ① 类目：一次取全表，构建 categoryId -> 类目实体 ----
        Map<Long, RepairCategory> categoryMap = new HashMap<>();
        List<RepairCategory> categories = repairCategoryMapper.selectList(null);
        for (RepairCategory category : categories) {
            categoryMap.put(category.getCategoryId(), category);
        }

        // ---- ② 用户：一次性批量取本页涉及的居民与维修人员 ----
        Set<Long> userIds = new HashSet<>();
        for (RepairOrder order : orders) {
            addIfNotNull(userIds, order.getResidentId());
            addIfNotNull(userIds, order.getWorkerId());
        }
        Map<Long, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            for (SysUser user : users) {
                userNameMap.put(user.getUserId(), user.getRealName());
            }
        }
        return new NameContext(categoryMap, userNameMap);
    }

    /** 批量装载用户实体，供验收人姓名与角色装配使用 */
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

    /** 把非空 ID 收集进集合，避免把 null 传给 selectBatchIds 造成空指针 */
    private void addIfNotNull(Set<Long> target, Long id) {
        if (id != null) {
            target.add(id);
        }
    }

    // ==================================================================================
    // 十、私有方法 —— 角色判定的安全封装
    // ==================================================================================

    /** 是否居民（大小写不敏感，空值安全） */
    private boolean isResident(String role) {
        return RoleEnum.RESIDENT.getCode().equalsIgnoreCase(role);
    }

    /** 是否系统管理员 */
    private boolean isAdmin(String role) {
        return RoleEnum.ADMIN.getCode().equalsIgnoreCase(role);
    }

    /**
     * 名称装配上下文：把「类目实体表」与「用户名表」封装在一起，
     * 让转换器调用处保持简洁（与模块二 {@code RepairOrderServiceImpl.NameContext} 思路一致）。
     */
    private static final class NameContext {

        private final Map<Long, RepairCategory> categoryMap;
        private final Map<Long, String> userNameMap;

        private NameContext(Map<Long, RepairCategory> categoryMap, Map<Long, String> userNameMap) {
            this.categoryMap = categoryMap;
            this.userNameMap = userNameMap;
        }

        /** 取类目展示名，二级类目拼成「父类目 / 子类目」（接口文档 4.3.2 示例口径） */
        String categoryName(Long categoryId) {
            if (categoryId == null) {
                return null;
            }
            RepairCategory category = categoryMap.get(categoryId);
            if (category == null) {
                return null;
            }
            Long parentId = category.getParentId();
            if (parentId != null && parentId != CategoryConstants.ROOT_PARENT_ID) {
                RepairCategory parent = categoryMap.get(parentId);
                if (parent != null) {
                    return parent.getCategoryName() + " / " + category.getCategoryName();
                }
            }
            return category.getCategoryName();
        }

        /** 取用户真实姓名；ID 为空或用户已不存在时返回 null */
        String userName(Long userId) {
            return userId == null ? null : userNameMap.get(userId);
        }
    }
}
