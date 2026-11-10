package com.community.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.repair.common.BusinessException;
import com.community.repair.common.PageResult;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.CategoryConstants;
import com.community.repair.constant.OrderConstants;
import com.community.repair.constant.UserConstants;
import com.community.repair.convert.OrderConverter;
import com.community.repair.dto.OrderCancelDTO;
import com.community.repair.dto.OrderCreateDTO;
import com.community.repair.dto.OrderQueryDTO;
import com.community.repair.dto.OrderResubmitDTO;
import com.community.repair.dto.OrderUpdateDTO;
import com.community.repair.entity.RepairCategory;
import com.community.repair.entity.RepairOrder;
import com.community.repair.entity.SysUser;
import com.community.repair.enums.RoleEnum;
import com.community.repair.mapper.RepairCategoryMapper;
import com.community.repair.mapper.RepairOrderMapper;
import com.community.repair.mapper.SysUserMapper;
import com.community.repair.security.LoginUser;
import com.community.repair.security.UserContext;
import com.community.repair.service.RepairOrderService;
import com.community.repair.vo.OrderCancelVO;
import com.community.repair.vo.OrderCreateVO;
import com.community.repair.vo.OrderDetailVO;
import com.community.repair.vo.OrderListVO;
import com.community.repair.vo.OrderOperateVO;
import com.community.repair.vo.OrderResubmitVO;
import com.community.repair.vo.OrderUpdateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报修工单提交模块 业务实现。
 *
 * <p>本类集中承载需求文档 5.2 节 FR-2-01 ~ FR-2-09 与 BR-2-01 ~ BR-2-09 的全部业务规则，
 * 每个方法上方的注释都标注了对应的规则编号，便于论文与答辩溯源。</p>
 *
 * <p><b>复用的基础组件（不重复创建）</b>：</p>
 * <ul>
 *   <li>统一响应体 {@code Result} / 分页结构 {@code PageResult}；</li>
 *   <li>业务异常 {@code BusinessException} + 全局异常处理器 {@code GlobalExceptionHandler}；</li>
 *   <li>状态码字典 {@code ResultCode}；</li>
 *   <li>鉴权与角色校验 {@code JwtInterceptor} / {@code @RequireRole} / {@code UserContext}
 *       —— <b>用户身份一律通过 {@link UserContext#require()} 获取，不接受前端传入</b>（BR-2-06）；</li>
 *   <li>字段自动填充 {@code MyMetaObjectHandler}、分页插件 {@code PaginationInnerInterceptor}；</li>
 *   <li>常量与枚举组织方式沿用模块零 {@code UserConstants}、模块一 {@code CategoryConstants}；
 *       实体 -> VO 转换沿用 {@code CategoryConverter} 的静态方法风格。</li>
 * </ul>
 *
 * <p><b>与下游模块的衔接（任务核心要求之三）</b>：本模块写库时严格使用建表脚本的字段与取值，
 * 工单初始状态恒为 8 待审核；下游「工单派单调度模块」只需按
 * {@code status = 8} 取待审核池、调用 D-12 审核（8 → 0）即可接入派单链路，
 * <b>无需对本模块的数据结构做任何适配</b>。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepairOrderServiceImpl implements RepairOrderService {

    private final RepairOrderMapper repairOrderMapper;
    /** 上游模块一的类目字典：提交前校验类目存在性与启用状态（BR-1-05） */
    private final RepairCategoryMapper repairCategoryMapper;
    /** 只读联查用户姓名（居民 / 维修人员 / 派单管理员 / 驳回管理员） */
    private final SysUserMapper sysUserMapper;

    /** 时间格式：与全局 JacksonConfig 保持一致 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 日期格式：查询条件允许只传日期，自动补 00:00:00 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 允许排序的字段白名单（下划线命名 -> 实体属性引用）。
     *
     * <p>作用：{@code orderBy} 是前端可控入参，若直接拼接到 SQL 会产生注入风险。
     * 这里用白名单映射为 MyBatis-Plus 的 Lambda 列引用，<b>参数值不参与 SQL 拼接</b>，
     * 从根本上杜绝注入，同时保证排序字段一定存在于表中。</p>
     */
    private static final Map<String, SFunction<RepairOrder, ?>> SORT_COLUMNS;

    static {
        Map<String, SFunction<RepairOrder, ?>> map = new LinkedHashMap<>();
        map.put("create_time", RepairOrder::getCreateTime);
        map.put("update_time", RepairOrder::getUpdateTime);
        map.put("urgency", RepairOrder::getUrgency);
        map.put("status", RepairOrder::getStatus);
        map.put("expect_time", RepairOrder::getExpectTime);
        map.put("total_cost", RepairOrder::getTotalCost);
        map.put("order_id", RepairOrder::getOrderId);
        SORT_COLUMNS = Collections.unmodifiableMap(map);
    }

    /**
     * 工单编号流水号生成锁。
     *
     * <p>BR-2-05 要求编号为「BX + yyyyMMdd + 4 位当日流水号」且全局唯一。
     * 生成过程是「查当日最大值 → 加一」，天然存在并发竞态；
     * 这里用 JVM 内锁串行化该动作，并由数据库唯一索引 {@code uk_order_no} 作为最终兜底，
     * 保证任何情况下都不会产生重复编号（毕设单实例部署场景已足够）。</p>
     */
    private static final Object ORDER_NO_LOCK = new Object();

    // ==================================================================================
    // 一、查询类接口（O-02、O-03，全部登录用户可访问，按角色自动限定数据范围）
    // ==================================================================================

    /**
     * O-02 工单分页列表。
     *
     * <p>对应 FR-2-04、FR-3-01、FR-3-11、FR-4-01、FR-5-01 与需求文档 3.3 数据可见范围规则。</p>
     *
     * <p><b>数据范围自动限定（服务端强制，不依赖前端传参）</b>：</p>
     * <ul>
     *   <li>居民：强制 {@code resident_id = 当前用户ID}，即「我的报修」；</li>
     *   <li>维修人员：强制 {@code worker_id = 当前用户ID}，即「我的工单」；</li>
     *   <li>管理员：不限定，可按 {@code residentId} / {@code workerId} 主动筛选。</li>
     * </ul>
     * <p>居民或维修人员若传入 {@code residentId} / {@code workerId}，直接返回 403
     * （接口文档 4.3.2 状态码说明：越权筛选）。</p>
     *
     * <p><b>状态全流程可见</b>：本方法不限制状态取值，返回 10 态字典中的任意状态，
     * 且每次实时查库——下游派单调度 / 维修进度 / 验收 / 评价模块一旦更新状态，
     * 本列表立即呈现，无需任何联动改造（任务核心要求之四）。</p>
     */
    @Override
    public PageResult<OrderListVO> pageOrders(OrderQueryDTO query) {
        LoginUser current = UserContext.require();
        String role = current.getRole();
        Long userId = current.getUserId();

        // 分页参数保护：pageNum >= 1，1 <= pageSize <= 100（接口文档 2.5 节）
        int pageNum = (query.getPageNum() == null || query.getPageNum() < 1) ? 1 : query.getPageNum();
        int pageSize = (query.getPageSize() == null || query.getPageSize() < 1)
                ? OrderConstants.DEFAULT_PAGE_SIZE
                : Math.min(query.getPageSize(), OrderConstants.MAX_PAGE_SIZE);

        // 越权筛选拦截：residentId / workerId 仅管理员可用（接口文档 4.3.2，403）
        boolean isAdmin = RoleEnum.ADMIN.getCode().equalsIgnoreCase(role);
        if (!isAdmin && query.getResidentId() != null) {
            throw new BusinessException(ResultCode.FORBIDDEN, OrderConstants.MSG_RESIDENT_FILTER_FORBIDDEN);
        }
        if (!isAdmin && query.getWorkerId() != null) {
            throw new BusinessException(ResultCode.FORBIDDEN, OrderConstants.MSG_WORKER_FILTER_FORBIDDEN);
        }

        // 状态参数解析：支持单值或多值（英文逗号分隔），越界返回 400
        List<Integer> statusList = parseStatusList(query.getStatus());

        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();

        // ---- ① 角色数据范围（需求文档 3.3，服务端强制注入） ----
        if (RoleEnum.RESIDENT.getCode().equalsIgnoreCase(role)) {
            // 居民只能看到本人提交的工单
            wrapper.eq(RepairOrder::getResidentId, userId);
        } else if (RoleEnum.REPAIR.getCode().equalsIgnoreCase(role)) {
            // 维修人员只能看到当前指派给本人的工单
            wrapper.eq(RepairOrder::getWorkerId, userId);
        } else {
            // 管理员：可主动按居民 / 维修人员筛选
            wrapper.eq(query.getResidentId() != null, RepairOrder::getResidentId, query.getResidentId());
            wrapper.eq(query.getWorkerId() != null, RepairOrder::getWorkerId, query.getWorkerId());
        }

        // ---- ② 通用筛选条件 ----
        if (statusList != null && !statusList.isEmpty()) {
            wrapper.in(RepairOrder::getStatus, statusList);
        }
        wrapper.eq(query.getCategoryId() != null, RepairOrder::getCategoryId, query.getCategoryId());
        wrapper.eq(query.getUrgency() != null, RepairOrder::getUrgency, query.getUrgency());

        // 关键字：模糊匹配 orderNo / title / address / contactName（接口文档 4.3.2）
        // 用 and(...) 包裹，保证 OR 条件整体不破坏前面的角色数据范围限定
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            if (keyword.length() > OrderConstants.KEYWORD_MAX_LENGTH) {
                throw new BusinessException(ResultCode.BAD_REQUEST, OrderConstants.MSG_KEYWORD_TOO_LONG);
            }
            wrapper.and(w -> w.like(RepairOrder::getOrderNo, keyword)
                    .or().like(RepairOrder::getTitle, keyword)
                    .or().like(RepairOrder::getAddress, keyword)
                    .or().like(RepairOrder::getContactName, keyword));
        }

        // 提交时间区间，字段限定为 create_time
        LocalDateTime startTime = parseTime(query.getStartTime(), "提交时间起");
        LocalDateTime endTime = parseTime(query.getEndTime(), "提交时间止");
        wrapper.ge(startTime != null, RepairOrder::getCreateTime, startTime);
        wrapper.le(endTime != null, RepairOrder::getCreateTime, endTime);

        // ---- ③ 排序 ----
        boolean pendingDispatchView = statusList != null && statusList.contains(OrderConstants.STATUS_PENDING_DISPATCH);
        applySort(wrapper, query, pendingDispatchView);

        Page<RepairOrder> page = repairOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 批量装配名称，避免逐条查询导致的 N+1（与模块一 loadParentNameMap 的做法一致）
        NameContext names = loadNames(page.getRecords());
        return PageResult.of(page, order -> OrderConverter.toListVO(
                order,
                names.categoryName(order.getCategoryId()),
                names.userName(order.getResidentId()),
                names.userName(order.getWorkerId())));
    }

    /**
     * O-03 工单详情。
     *
     * <p>对应 FR-2-05、FR-5-02 与 BR-2-06 数据归属校验。</p>
     *
     * <p><b>数据归属三重校验</b>（禁止仅依赖前端隐藏菜单）：居民仅可查看本人工单、
     * 维修人员仅可查看本人受派工单、管理员可查看全部；不符时返回 403 且<b>不返回任何数据</b>。</p>
     *
     * <p>状态为 9「已驳回」时额外返回驳回原因、驳回人与驳回时间（FR-2-05）；
     * 其余状态这四个字段一律为 {@code null}。</p>
     */
    @Override
    public OrderDetailVO getOrderDetail(Long orderId) {
        LoginUser current = UserContext.require();
        RepairOrder order = selectExistingOrder(orderId);
        // 数据归属校验：不通过直接 403，连工单是否存在以外的信息都不外泄
        checkViewPermission(order, current);

        // 「是否已评价」为派生字段，用于前端控制评价入口显隐（一单一评 BR-6-02）
        boolean evaluated = repairOrderMapper.countEvaluationByOrderId(orderId) > 0;
        OrderOperateVO canOperate = OrderConverter.buildOperate(
                order, current.getUserId(), current.getRole(), evaluated);

        // 装配关联名称：类目、居民、维修人员、派单管理员，以及仅状态 9 才需要的驳回管理员
        NameContext names = loadNames(List.of(order));
        boolean rejected = order.getStatus() != null && order.getStatus() == OrderConstants.STATUS_REJECTED;
        String rejectorName = rejected ? names.userName(order.getRejectorId()) : null;

        return OrderConverter.toDetailVO(
                order,
                names.categoryName(order.getCategoryId()),
                names.userName(order.getResidentId()),
                names.userName(order.getWorkerId()),
                names.userName(order.getDispatcherId()),
                rejectorName,
                evaluated,
                canOperate);
    }

    // ==================================================================================
    // 二、写入类接口（O-01、O-04、O-05、O-06）
    // ==================================================================================

    /**
     * O-01 提交报修工单。
     *
     * <p>对应 FR-2-01、FR-2-02、FR-2-03、FR-2-07、FR-2-08 与
     * BR-2-01 ~ BR-2-03、BR-2-08。</p>
     *
     * <p><b>三条关键约定</b>：</p>
     * <ol>
     *   <li>{@code resident_id} 由 JWT 解析后自动关联，<b>不接受前端传入</b>（BR-2-06）；</li>
     *   <li>{@code status} 固定写入 <b>8 待审核</b>（BR-2-04 / BR-3-10），提交后进入管理员审核工作台；</li>
     *   <li>{@code reject_reason} / {@code rejector_id} / {@code reject_time} 本次一律保持
     *       {@code NULL}；派单类与验收类字段同样保持 {@code NULL}，等待下游模块回填。</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO createOrder(OrderCreateDTO dto) {
        LoginUser current = UserContext.require();
        // 兜底防御：@RequireRole 已在拦截器层拦截非居民，这里再做一次 Service 层校验
        if (!RoleEnum.RESIDENT.getCode().equalsIgnoreCase(current.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, OrderConstants.MSG_ROLE_NOT_RESIDENT);
        }

        // BR-1-05 + 接口文档 4.3.1：类目必须存在（404）且为启用状态（409）
        validateCategoryUsable(dto.getCategoryId());

        RepairOrder order = new RepairOrder();
        order.setOrderNo(generateOrderNo());
        order.setResidentId(current.getUserId());          // BR-2-06：服务端关联，不接受前端传入
        order.setCategoryId(dto.getCategoryId());
        order.setTitle(normalizeTitle(dto.getTitle()));
        order.setDescription(normalizeDescription(dto.getDescription()));
        order.setImages(normalizeImages(dto.getImages()));
        order.setAddress(normalizeAddress(dto.getAddress()));
        order.setContactName(normalizeContactName(dto.getContactName()));
        order.setContactPhone(normalizeContactPhone(dto.getContactPhone()));
        order.setUrgency(normalizeUrgency(dto.getUrgency()));
        order.setExpectTime(validateExpectTime(dto.getExpectTime()));
        // BR-2-04：初始状态固定为 8 待审核
        order.setStatus(OrderConstants.STATUS_PENDING_AUDIT);
        // 新建工单维修费用为 0.00，由下游维修进度模块累加
        order.setTotalCost(BigDecimal.ZERO);
        order.setDeleted(0);

        repairOrderMapper.insert(order);
        log.info("居民 {} 提交报修工单成功：orderNo={}, categoryId={}, status={}(待审核)",
                current.getUserId(), order.getOrderNo(), order.getCategoryId(), order.getStatus());
        return OrderConverter.toCreateVO(order);
    }

    /**
     * O-04 修改工单。
     *
     * <p>对应 FR-2-01。允许修改的状态为 <b>8 待审核</b> 与 <b>0 待派单</b>；
     * <b>修改不改变工单状态</b>（8 → 8，0 → 0），不触发重新审核。</p>
     *
     * <p>状态为 9 已驳回时引导使用 O-06 重新提交接口（提示语按接口文档 4.3.4 区分）；
     * 已派单（1）及之后的状态提示「当前状态的工单不可编辑」。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderUpdateVO updateOrder(Long orderId, OrderUpdateDTO dto) {
        LoginUser current = UserContext.require();
        RepairOrder order = selectExistingOrder(orderId);
        // 数据归属校验：只能修改本人提交的工单
        checkOwnership(order, current);

        // 状态域校验（接口文档 4.3.4，409）：仅 8 待审核与 0 待派单可修改
        int status = order.getStatus() == null ? -1 : order.getStatus();
        if (status == OrderConstants.STATUS_REJECTED) {
            throw new BusinessException(ResultCode.CONFLICT, OrderConstants.MSG_STATUS_REJECTED_USE_RESUBMIT);
        }
        if (status != OrderConstants.STATUS_PENDING_AUDIT
                && status != OrderConstants.STATUS_PENDING_DISPATCH) {
            throw new BusinessException(ResultCode.CONFLICT, OrderConstants.MSG_STATUS_NOT_MODIFIABLE);
        }

        // 类目变更同样要校验存在性与启用状态
        if (dto.getCategoryId() != null) {
            validateCategoryUsable(dto.getCategoryId());
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId);
        // 局部更新：仅把「前端确实传了值」的字段写入 UPDATE 语句
        if (dto.getCategoryId() != null) {
            wrapper.set(RepairOrder::getCategoryId, dto.getCategoryId());
            order.setCategoryId(dto.getCategoryId());
        }
        if (dto.getTitle() != null) {
            String title = normalizeTitle(dto.getTitle());
            wrapper.set(RepairOrder::getTitle, title);
            order.setTitle(title);
        }
        if (dto.getDescription() != null) {
            String description = normalizeDescription(dto.getDescription());
            wrapper.set(RepairOrder::getDescription, description);
            order.setDescription(description);
        }
        if (dto.getImages() != null) {
            // 允许传空字符串表示清空照片，因此这里以「是否传了该字段」而非「是否有内容」判断
            String images = normalizeImages(dto.getImages());
            wrapper.set(RepairOrder::getImages, images);
            order.setImages(images);
        }
        if (dto.getAddress() != null) {
            String address = normalizeAddress(dto.getAddress());
            wrapper.set(RepairOrder::getAddress, address);
            order.setAddress(address);
        }
        if (dto.getContactName() != null) {
            String contactName = normalizeContactName(dto.getContactName());
            wrapper.set(RepairOrder::getContactName, contactName);
            order.setContactName(contactName);
        }
        if (dto.getContactPhone() != null) {
            String contactPhone = normalizeContactPhone(dto.getContactPhone());
            wrapper.set(RepairOrder::getContactPhone, contactPhone);
            order.setContactPhone(contactPhone);
        }
        if (dto.getUrgency() != null) {
            Integer urgency = normalizeUrgency(dto.getUrgency());
            wrapper.set(RepairOrder::getUrgency, urgency);
            order.setUrgency(urgency);
        }
        if (dto.getExpectTime() != null) {
            LocalDateTime expectTime = validateExpectTime(dto.getExpectTime());
            wrapper.set(RepairOrder::getExpectTime, expectTime);
            order.setExpectTime(expectTime);
        }
        // 方案 A 要求 update_time 由业务代码显式维护（@TableLogic 追加 deleted = 0）
        wrapper.set(RepairOrder::getUpdateTime, now);
        repairOrderMapper.update(null, wrapper);

        // 修改不改变状态（8 → 8 或 0 → 0），因此这里沿用原状态构造响应
        order.setUpdateTime(now);
        log.info("居民 {} 修改工单成功：orderId={}, 状态保持 {}",
                current.getUserId(), orderId, OrderConverter.statusName(order.getStatus()));
        return OrderConverter.toUpdateVO(order);
    }

    /**
     * O-05 撤销工单。
     *
     * <p>对应 FR-2-06、BR-2-07。允许撤销的状态严格为 <b>8 待审核 / 0 待派单 / 9 已驳回</b>
     * （需求文档 3.2 权限矩阵：「撤销未派单的工单」），其余状态一律拒绝。</p>
     *
     * <p>权限：居民只能撤销本人提交的工单；管理员可撤销任意工单（接口文档 4.3.5 权限角色）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCancelVO cancelOrder(Long orderId, OrderCancelDTO dto) {
        LoginUser current = UserContext.require();
        RepairOrder order = selectExistingOrder(orderId);

        boolean isAdmin = RoleEnum.ADMIN.getCode().equalsIgnoreCase(current.getRole());
        // 管理员可撤销任意工单；非管理员（居民）必须是工单的提交人
        if (!isAdmin) {
            checkOwnership(order, current);
        }

        // 状态域校验（BR-2-07，409）
        int status = order.getStatus() == null ? -1 : order.getStatus();
        boolean cancelable = Arrays.stream(OrderConstants.CANCELABLE_STATUSES).anyMatch(s -> s == status);
        if (!cancelable) {
            throw new BusinessException(ResultCode.CONFLICT, OrderConstants.MSG_STATUS_NOT_CANCELABLE);
        }

        LocalDateTime now = LocalDateTime.now();
        String cancelReason = dto.getCancelReason() == null ? null : dto.getCancelReason().trim();
        // 二次校验：@NotBlank / @Size 已拦一层，这里兜住纯空格的情况
        if (!StringUtils.hasText(cancelReason)
                || cancelReason.length() < OrderConstants.CANCEL_REASON_MIN_LENGTH
                || cancelReason.length() > OrderConstants.CANCEL_REASON_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "撤销原因长度须为 5~200 个字符");
        }

        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId)
                .set(RepairOrder::getStatus, OrderConstants.STATUS_CANCELED)
                .set(RepairOrder::getCancelReason, cancelReason)
                .set(RepairOrder::getUpdateTime, now);
        repairOrderMapper.update(null, wrapper);

        order.setStatus(OrderConstants.STATUS_CANCELED);
        order.setCancelReason(cancelReason);
        order.setUpdateTime(now);
        log.info("用户 {}（{}）撤销工单成功：orderId={}，原因={}",
                current.getUserId(), current.getRole(), orderId, cancelReason);
        return OrderConverter.toCancelVO(order);
    }

    /**
     * O-06 驳回工单修改后重新提交（V2.0 新增）。
     *
     * <p>对应 FR-2-09、BR-2-09、BR-3-10。允许操作的状态<b>严格为 9 已驳回</b>；
     * 成功后状态由 9 回到 <b>8 待审核</b>，重新进入管理员审核队列。</p>
     *
     * <p><b>提交成功后清空驳回快照</b>：{@code reject_reason} / {@code rejector_id} /
     * {@code reject_time} 一并置为 {@code NULL}，与数据库 V2.0
     * 「驳回字段仅状态 9 时有值」的口径保持一致。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResubmitVO resubmitOrder(Long orderId, OrderResubmitDTO dto) {
        LoginUser current = UserContext.require();
        RepairOrder order = selectExistingOrder(orderId);
        // 数据归属校验：只能重新提交本人提交的工单
        checkOwnership(order, current);

        // 状态域校验（BR-2-09，409）：仅「已驳回」可重新提交
        if (order.getStatus() == null || order.getStatus() != OrderConstants.STATUS_REJECTED) {
            throw new BusinessException(ResultCode.CONFLICT, OrderConstants.MSG_STATUS_NOT_RESUBMITTABLE);
        }

        // 至少须提交一个可修改字段，否则 400（接口文档 4.3.6）
        if (isAllModifyFieldsEmpty(dto)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, OrderConstants.MSG_RESUBMIT_EMPTY);
        }

        if (dto.getCategoryId() != null) {
            validateCategoryUsable(dto.getCategoryId());
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<RepairOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairOrder::getOrderId, orderId);
        if (dto.getCategoryId() != null) {
            wrapper.set(RepairOrder::getCategoryId, dto.getCategoryId());
            order.setCategoryId(dto.getCategoryId());
        }
        if (dto.getTitle() != null) {
            String title = normalizeTitle(dto.getTitle());
            wrapper.set(RepairOrder::getTitle, title);
            order.setTitle(title);
        }
        if (dto.getDescription() != null) {
            String description = normalizeDescription(dto.getDescription());
            wrapper.set(RepairOrder::getDescription, description);
            order.setDescription(description);
        }
        if (dto.getImages() != null) {
            String images = normalizeImages(dto.getImages());
            wrapper.set(RepairOrder::getImages, images);
            order.setImages(images);
        }
        if (dto.getAddress() != null) {
            String address = normalizeAddress(dto.getAddress());
            wrapper.set(RepairOrder::getAddress, address);
            order.setAddress(address);
        }
        if (dto.getContactName() != null) {
            String contactName = normalizeContactName(dto.getContactName());
            wrapper.set(RepairOrder::getContactName, contactName);
            order.setContactName(contactName);
        }
        if (dto.getContactPhone() != null) {
            String contactPhone = normalizeContactPhone(dto.getContactPhone());
            wrapper.set(RepairOrder::getContactPhone, contactPhone);
            order.setContactPhone(contactPhone);
        }
        if (dto.getUrgency() != null) {
            Integer urgency = normalizeUrgency(dto.getUrgency());
            wrapper.set(RepairOrder::getUrgency, urgency);
            order.setUrgency(urgency);
        }
        if (dto.getExpectTime() != null) {
            LocalDateTime expectTime = validateExpectTime(dto.getExpectTime());
            wrapper.set(RepairOrder::getExpectTime, expectTime);
            order.setExpectTime(expectTime);
        }

        // 状态回到 8 待审核（BR-3-10），并清空驳回快照（三个字段一并置 NULL）
        wrapper.set(RepairOrder::getStatus, OrderConstants.STATUS_PENDING_AUDIT)
                .set(RepairOrder::getRejectReason, null)
                .set(RepairOrder::getRejectorId, null)
                .set(RepairOrder::getRejectTime, null)
                .set(RepairOrder::getUpdateTime, now);
        repairOrderMapper.update(null, wrapper);

        order.setStatus(OrderConstants.STATUS_PENDING_AUDIT);
        order.setRejectReason(null);
        order.setRejectorId(null);
        order.setRejectTime(null);
        order.setUpdateTime(now);
        log.info("居民 {} 重新提交被驳工单成功：orderId={}，状态 9 → 8，驳回快照已清空",
                current.getUserId(), orderId);
        return OrderConverter.toResubmitVO(order);
    }

    // ==================================================================================
    // 三、私有工具方法 —— 数据获取与权限校验
    // ==================================================================================

    /**
     * 按主键查询未删除的工单，不存在时抛出 404。
     * <p>{@code @TableLogic} 会自动追加 {@code deleted = 0}，逻辑删除的记录一律视为不存在。</p>
     */
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
     * 数据归属校验（O-03 查看场景）：居民看本人、维修人员看本人受派、管理员看全部。
     * <p>不符合时抛 403 且提示语统一为「无权限查看该工单」（接口文档 4.3.3）。</p>
     */
    private void checkViewPermission(RepairOrder order, LoginUser current) {
        if (!hasViewRight(order, current)) {
            log.warn("越权查看工单被拦截：userId={}, role={}, orderId={}",
                    current.getUserId(), current.getRole(), order.getOrderId());
            throw new BusinessException(ResultCode.FORBIDDEN, OrderConstants.MSG_NO_PERMISSION_VIEW);
        }
    }

    /** 判断当前用户是否有权查看指定工单 */
    private boolean hasViewRight(RepairOrder order, LoginUser current) {
        String role = current.getRole();
        Long userId = current.getUserId();
        if (RoleEnum.ADMIN.getCode().equalsIgnoreCase(role)) {
            return true;
        }
        if (RoleEnum.RESIDENT.getCode().equalsIgnoreCase(role)) {
            return order.getResidentId() != null && order.getResidentId().equals(userId);
        }
        if (RoleEnum.REPAIR.getCode().equalsIgnoreCase(role)) {
            return order.getWorkerId() != null && order.getWorkerId().equals(userId);
        }
        return false;
    }

    /**
     * 数据归属校验（写入场景）：仅工单提交人本人可操作。
     * <p>不符合时抛 403，提示「无权限操作该工单」（接口文档 4.3.4 / 4.3.6）。</p>
     */
    private void checkOwnership(RepairOrder order, LoginUser current) {
        boolean isOwner = order.getResidentId() != null
                && order.getResidentId().equals(current.getUserId());
        if (!isOwner) {
            log.warn("越权操作工单被拦截：userId={}, role={}, orderId={}",
                    current.getUserId(), current.getRole(), order.getOrderId());
            throw new BusinessException(ResultCode.FORBIDDEN, OrderConstants.MSG_NO_PERMISSION_OPERATE);
        }
    }

    /**
     * 上游类目校验：必须存在且为启用状态（接口文档 4.3.1 状态码 404 / 409，BR-1-05）。
     *
     * <p>这是本模块与<b>上游模块一</b>的衔接点：类目字典由模块一维护，
     * 本模块只做只读校验，不重复实现类目的增删改。</p>
     */
    private void validateCategoryUsable(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择报修类目");
        }
        RepairCategory category = repairCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, OrderConstants.MSG_CATEGORY_NOT_FOUND);
        }
        if (category.getStatus() == null || category.getStatus() != CategoryConstants.STATUS_ENABLED) {
            // 停用类目不允许被新工单选择（BR-1-05）
            throw new BusinessException(ResultCode.CONFLICT, OrderConstants.MSG_CATEGORY_DISABLED);
        }
    }

    // ==================================================================================
    // 四、私有工具方法 —— 字段二次校验与规范化
    //     说明：@Valid 已做第一层校验，这里做第二层（业务语义更精确），
    //           与模块一「校验放 Service 层以保证提示语准确」的做法保持一致。
    // ==================================================================================

    /** 报修标题规范化：去空格 + 2~100 字校验 */
    private String normalizeTitle(String title) {
        String value = title == null ? null : title.trim();
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写报修标题");
        }
        if (value.length() < OrderConstants.TITLE_MIN_LENGTH
                || value.length() > OrderConstants.TITLE_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "报修标题长度须为 2~100 个字符");
        }
        return value;
    }

    /** 故障描述规范化：去空格 + 10~500 字校验（BR-2-03） */
    private String normalizeDescription(String description) {
        String value = description == null ? null : description.trim();
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写故障详细描述");
        }
        if (value.length() < OrderConstants.DESCRIPTION_MIN_LENGTH
                || value.length() > OrderConstants.DESCRIPTION_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "故障描述长度需在 10~500 字之间");
        }
        return value;
    }

    /** 上门维修地址规范化：去空格 + 2~255 字校验 */
    private String normalizeAddress(String address) {
        String value = address == null ? null : address.trim();
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写上门维修地址");
        }
        if (value.length() < OrderConstants.ADDRESS_MIN_LENGTH
                || value.length() > OrderConstants.ADDRESS_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上门维修地址长度须为 2~255 个字符");
        }
        return value;
    }

    /** 联系人姓名规范化：去空格 + 2~50 字校验 */
    private String normalizeContactName(String contactName) {
        String value = contactName == null ? null : contactName.trim();
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写联系人姓名");
        }
        if (value.length() < OrderConstants.CONTACT_NAME_MIN_LENGTH
                || value.length() > OrderConstants.CONTACT_NAME_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "联系人姓名长度须为 2~50 个字符");
        }
        return value;
    }

    /** 联系人电话规范化：11 位手机号（BR-2-02） */
    private String normalizeContactPhone(String contactPhone) {
        String value = contactPhone == null ? null : contactPhone.trim();
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写联系人电话");
        }
        if (!value.matches(UserConstants.REGEX_PHONE)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请输入正确的 11 位手机号");
        }
        return value;
    }

    /**
     * 现场照片规范化：去空项 + 张数与总长校验（FR-2-07）。
     *
     * @return 规范后的逗号分隔路径串；无有效图片时返回 {@code null}
     */
    private String normalizeImages(String images) {
        if (!StringUtils.hasText(images)) {
            // 未上传照片或主动清空，统一落库为 null（images 字段允许为 NULL）
            return null;
        }
        // 按英文逗号切分，过滤空串，避免出现 "a,,b" 这样的脏数据
        List<String> parts = Arrays.stream(images.split(OrderConstants.IMAGES_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (parts.isEmpty()) {
            return null;
        }
        if (parts.size() > OrderConstants.IMAGES_MAX_COUNT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, OrderConstants.MSG_IMAGES_TOO_MANY);
        }
        String joined = String.join(OrderConstants.IMAGES_SEPARATOR, parts);
        if (joined.length() > OrderConstants.IMAGES_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, OrderConstants.MSG_IMAGES_TOO_LENGTH);
        }
        return joined;
    }

    /** 紧急程度规范化：null 视为 0 普通，取值只能是 0 / 1（FR-2-08） */
    private Integer normalizeUrgency(Integer urgency) {
        if (urgency == null) {
            return OrderConstants.URGENCY_NORMAL;
        }
        if (urgency != OrderConstants.URGENCY_NORMAL && urgency != OrderConstants.URGENCY_URGENT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, OrderConstants.MSG_URGENCY_INVALID);
        }
        return urgency;
    }

    /** 期望上门时间校验：不得早于当前时间（BR-2-08）；未填写返回 null */
    private LocalDateTime validateExpectTime(LocalDateTime expectTime) {
        if (expectTime == null) {
            return null;
        }
        if (expectTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, OrderConstants.MSG_EXPECT_TIME_PAST);
        }
        return expectTime;
    }

    /** 判断 O-06 重新提交请求中是否<b>一个可修改字段都没有传</b>（对应 400 触发场景） */
    private boolean isAllModifyFieldsEmpty(OrderResubmitDTO dto) {
        return dto.getCategoryId() == null
                && dto.getTitle() == null
                && dto.getDescription() == null
                && dto.getImages() == null
                && dto.getAddress() == null
                && dto.getContactName() == null
                && dto.getContactPhone() == null
                && dto.getUrgency() == null
                && dto.getExpectTime() == null;
    }

    // ==================================================================================
    // 五、私有工具方法 —— 编号生成、条件解析与名称装配
    // ==================================================================================

    /**
     * 生成工单编号（BR-2-05）：{@code BX + yyyyMMdd + 4 位当日流水号}。
     *
     * <p>实现步骤：</p>
     * <ol>
     *   <li>按当前日期拼出前缀，如 {@code BX20260918}；</li>
     *   <li>查库取当日最大编号（{@code RepairOrderMapper.selectMaxOrderNoByPrefix}，
     *       走 {@code uk_order_no} 唯一索引，效率为 O(1)）；</li>
     *   <li>截取末 4 位加一，不足 4 位左侧补 0；当日无工单则从 0001 开始。</li>
     * </ol>
     * <p>并发安全：整体加 JVM 内锁串行化，配合数据库唯一索引兜底。</p>
     */
    private String generateOrderNo() {
        String prefix = OrderConstants.ORDER_NO_PREFIX
                + LocalDate.now().format(DateTimeFormatter.ofPattern(OrderConstants.ORDER_NO_DATE_PATTERN));
        synchronized (ORDER_NO_LOCK) {
            String maxOrderNo = repairOrderMapper.selectMaxOrderNoByPrefix(prefix);
            int nextSeq = 1;
            if (maxOrderNo != null && maxOrderNo.length() > prefix.length()) {
                try {
                    nextSeq = Integer.parseInt(maxOrderNo.substring(prefix.length())) + 1;
                } catch (NumberFormatException e) {
                    // 历史数据存在脏编号时退化为从 0001 重新尝试，由唯一索引兜底
                    log.warn("解析历史工单编号失败，按 0001 重新生成：maxOrderNo={}", maxOrderNo);
                    nextSeq = 1;
                }
            }
            if (nextSeq > OrderConstants.ORDER_NO_MAX_SEQ) {
                throw new BusinessException(ResultCode.ERROR, OrderConstants.MSG_ORDER_NO_LIMIT);
            }
            if (nextSeq < 1) {
                throw new BusinessException(ResultCode.ERROR, OrderConstants.MSG_ORDER_NO_FAIL);
            }
            return prefix + String.format("%0" + OrderConstants.ORDER_NO_SEQ_LENGTH + "d", nextSeq);
        }
    }

    /**
     * 解析状态筛选参数：支持单值（{@code 8}）与多值（{@code 0,8,9}）。
     *
     * @param status 原始参数
     * @return 状态列表；参数为空时返回 {@code null} 表示不筛选
     * @throws BusinessException 状态值非数字或超出 0~9 范围时抛 400
     */
    private List<Integer> parseStatusList(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        List<Integer> list = new ArrayList<>();
        for (String part : status.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) {
                continue;
            }
            int value;
            try {
                value = Integer.parseInt(item);
            } catch (NumberFormatException e) {
                throw new BusinessException(ResultCode.BAD_REQUEST, OrderConstants.MSG_STATUS_INVALID);
            }
            if (value < OrderConstants.STATUS_MIN || value > OrderConstants.STATUS_MAX) {
                throw new BusinessException(ResultCode.BAD_REQUEST, OrderConstants.MSG_STATUS_INVALID);
            }
            if (!list.contains(value)) {
                list.add(value);
            }
        }
        return list.isEmpty() ? null : list;
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
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldLabel + "：" + OrderConstants.MSG_TIME_FORMAT);
        }
    }

    /**
     * 装配排序条件（白名单校验 + 紧急工单置顶）。
     *
     * <p>规则（接口文档 4.3.2、BR-3-07）：</p>
     * <ul>
     *   <li>显式传 {@code orderBy = urgency} → 紧急程度降序（紧急在前）+ 提交时间升序；</li>
     *   <li>未传 {@code orderBy} 且处于待派单视图（含状态 0）→ 紧急工单默认置顶（BR-3-07）；</li>
     *   <li>其余情况 → 默认按 {@code create_time} 排序，方向取 {@code orderDir}（默认 desc）；</li>
     *   <li>{@code orderBy} 不在白名单内 → 400，防止 SQL 注入。</li>
     * </ul>
     *
     * @param wrapper             查询条件构造器
     * @param query               查询入参
     * @param pendingDispatchView 是否为待派单视图（状态包含 0）
     */
    private void applySort(LambdaQueryWrapper<RepairOrder> wrapper, OrderQueryDTO query,
                           boolean pendingDispatchView) {
        String orderBy = StringUtils.hasText(query.getOrderBy()) ? query.getOrderBy().trim() : null;

        // 紧急程度排序：无论 orderDir 如何，紧急(1) 恒在前，同级别按提交时间升序（BR-3-07）
        if ("urgency".equals(orderBy)) {
            wrapper.orderByDesc(RepairOrder::getUrgency).orderByAsc(RepairOrder::getCreateTime);
            return;
        }
        if (orderBy == null) {
            if (pendingDispatchView) {
                // 待派单池：紧急工单默认置顶
                wrapper.orderByDesc(RepairOrder::getUrgency).orderByAsc(RepairOrder::getCreateTime);
            } else {
                wrapper.orderByDesc(RepairOrder::getCreateTime);
            }
            return;
        }

        SFunction<RepairOrder, ?> column = SORT_COLUMNS.get(orderBy);
        if (column == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, OrderConstants.MSG_SORT_FIELD_INVALID);
        }
        boolean asc = "asc".equalsIgnoreCase(query.getOrderDir());
        wrapper.orderBy(true, asc, column);
    }

    /**
     * 批量装配列表 / 详情所需的关联名称，避免 N+1 查询。
     *
     * <p>做法与模块一 {@code loadParentNameMap} 一致：先收集本页记录涉及的全部外键 ID，
     * 再用 {@code selectBatchIds} 各查一次，做成 Map 供逐条取用。</p>
     *
     * <p><b>类目为何查全表？</b>{@code repair_category} 是基础数据字典表（初始化仅 9 条，
     * 规模恒定在个位/十位数），一次全量取出即可同时构建
     * 「类目ID -> 类目实体」，从而在一次查询内完成「子类目 + 父类目名」的拼装，
     * 优于按 parentId 二次查询。工单量增长不会影响该表规模。</p>
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

        // ---- ② 用户：一次性批量取本页涉及的全部用户（居民 / 维修 / 派单管理员 / 驳回管理员） ----
        Set<Long> userIds = new HashSet<>();
        for (RepairOrder order : orders) {
            addIfNotNull(userIds, order.getResidentId());
            addIfNotNull(userIds, order.getWorkerId());
            addIfNotNull(userIds, order.getDispatcherId());
            addIfNotNull(userIds, order.getRejectorId());
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

    /** 把非空的 ID 收集进集合，避免把 null 传给 selectBatchIds 造成空指针 */
    private void addIfNotNull(Set<Long> target, Long id) {
        if (id != null) {
            target.add(id);
        }
    }

    /**
     * 名称装配上下文：把「类目实体表」与「用户名表」封装在一起，
     * 让转换器调用处保持简洁（与 {@code CategoryConverter} 只接收 parentName 的思路一致）。
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
