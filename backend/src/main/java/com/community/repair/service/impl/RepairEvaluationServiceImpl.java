package com.community.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.repair.common.BusinessException;
import com.community.repair.common.PageResult;
import com.community.repair.common.ResultCode;
import com.community.repair.constant.CategoryConstants;
import com.community.repair.constant.EvaluationConstants;
import com.community.repair.constant.OrderConstants;
import com.community.repair.convert.EvaluationConverter;
import com.community.repair.convert.OrderConverter;
import com.community.repair.dto.EvaluationCreateDTO;
import com.community.repair.dto.EvaluationQueryDTO;
import com.community.repair.dto.EvaluationReplyDTO;
import com.community.repair.entity.RepairCategory;
import com.community.repair.entity.RepairEvaluation;
import com.community.repair.entity.RepairOrder;
import com.community.repair.entity.SysUser;
import com.community.repair.enums.RoleEnum;
import com.community.repair.mapper.RepairCategoryMapper;
import com.community.repair.mapper.RepairEvaluationMapper;
import com.community.repair.mapper.RepairOrderMapper;
import com.community.repair.mapper.SysUserMapper;
import com.community.repair.security.LoginUser;
import com.community.repair.security.UserContext;
import com.community.repair.service.RepairEvaluationService;
import com.community.repair.vo.EvaluationCreateVO;
import com.community.repair.vo.EvaluationDetailVO;
import com.community.repair.vo.EvaluationReplyVO;
import com.community.repair.vo.EvaluationVO;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 服务评价管理模块 业务实现（模块六）。
 *
 * <p>本类集中承载需求文档 5.6 节 FR-6-01 ~ FR-6-08 与 BR-6-01 ~ BR-6-07 的全部业务规则，
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
 *   <li>实体与 Mapper：{@code RepairOrder} / {@code RepairOrderMapper} 由模块二建立、
 *       {@code RepairCategory} 由模块一建立、{@code SysUser} 由模块零建立，本模块直接复用；</li>
 *   <li>字段自动填充 {@code MyMetaObjectHandler}。</li>
 * </ul>
 *
 * <p><b>并发控制与「一单一评」（BR-6-02 / NFR-R-02）</b>：E-01 的状态流转采用
 * 「条件更新 + 受影响行数校验」的乐观锁写法
 * （{@code UPDATE repair_order SET status = 6 WHERE order_id = ? AND status = 5}），
 * 受影响行数不为 1 即判定为并发冲突（另一请求已完成评价），返回 409；
 * 同时由 {@code repair_evaluation} 的唯一索引 {@code uk_order_id} 做数据库级兜底，
 * 极端并发下抛出的 {@link DuplicateKeyException} 同样转为 409。</p>
 *
 * <p><b>数据不可篡改（BR-6-05）</b>：本模块对 {@code repair_evaluation} 只有
 * 「新增评价（E-01）」与「一次性回复（E-04）」两种写路径，
 * 不存在任何修改评分 / 内容 / 删除评价的能力，保证评价数据的客观性。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepairEvaluationServiceImpl implements RepairEvaluationService {

    /** 评价表：评价记录的新增、查询与一次性回复（本模块建立） */
    private final RepairEvaluationMapper repairEvaluationMapper;

    /** 工单主表：状态 5 → 6 的流转与工单摘要装配（模块二既有 Mapper，本模块只调用不修改） */
    private final RepairOrderMapper repairOrderMapper;

    /** 类目字典：评价列表/详情的类目名称装配（模块一既有 Mapper） */
    private final RepairCategoryMapper repairCategoryMapper;

    /** 用户表：居民 / 维修人员姓名批量装配（模块零既有 Mapper） */
    private final SysUserMapper sysUserMapper;

    /** 时间格式：与全局 JacksonConfig 保持一致 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(EvaluationConstants.TIME_PATTERN);

    /** 日期格式：查询条件允许只传日期，自动补全时分秒 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(EvaluationConstants.DATE_PATTERN);

    // ==================================================================================
    // 一、E-01 提交服务评价
    // ==================================================================================

    /**
     * E-01 提交服务评价。
     *
     * <p>对应需求 FR-6-02、FR-6-03；BR-6-01 ~ BR-6-05；接口文档 4.7.1。</p>
     *
     * <p><b>完整校验链（按接口文档 4.7.1 状态码表逐条落地）</b>：</p>
     * <ol>
     *   <li>角色兜底：仅居民（403）；</li>
     *   <li>工单必须存在，否则 <b>404</b>；</li>
     *   <li>BR-6-06 前置校验：<b>仅工单报修人本人</b>可评价，否则 <b>403</b>
     *       「无权限操作该工单，仅工单报修人可提交评价」；</li>
     *   <li>BR-6-02：该工单尚未被评价，否则 <b>409</b>「该工单已评价，请勿重复提交」
     *       （先判重复、再判状态，保证「已评价的工单」给出的是重复评价提示而非状态提示）；</li>
     *   <li>BR-6-01：状态必须为 <b>5（已验收）</b>，否则 <b>409</b>「工单尚未验收，无法评价」；</li>
     *   <li>BR-6-03：三项评分均须为 1~5 的整数，否则 <b>400</b>；
     *       内容 ≤200 字、图片 ≤3 张且总长 ≤1000，否则 <b>400</b>。</li>
     * </ol>
     *
     * <p><b>状态流转（BR-6-04，业务闭环）</b>：评价落库 + 工单状态 <b>5 → 6（已完成）</b>，
     * 二者在同一事务内完成，任一失败整体回滚。
     * 状态 6 是「提交 → 审核 → 派单 → 接单 → 维修 → 验收 → 评价」全链路的<b>终点</b>，
     * 评价完成后业务链路彻底闭环。</p>
     *
     * <p><b>被评价人（worker_id）来源</b>：取自工单的 {@code worker_id}（当前受派维修人员），
     * <b>不由前端传入</b>，从机制上杜绝「评价到别人头上」。
     * 这与数据库设计 4.5 节「{@code worker_id} 应与对应工单的
     * {@code repair_order.worker_id} 保持一致（应用层校验）」的要求一致。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationCreateVO createEvaluation(EvaluationCreateDTO dto) {
        LoginUser current = requireResident();

        // ① 工单必须存在（404）
        RepairOrder order = selectExistingOrder(dto.getOrderId());
        // ② BR-6-06 前置校验：仅工单报修人本人可评价（403）
        assertOrderOwner(order, current);

        // ③ BR-6-02 重复评价前置断言（先判重，保证提示语精确）
        boolean alreadyEvaluated = repairEvaluationMapper.selectCount(
                new LambdaQueryWrapper<RepairEvaluation>().eq(RepairEvaluation::getOrderId, order.getOrderId())) > 0;
        if (alreadyEvaluated || isStatus(order.getStatus(), OrderConstants.STATUS_FINISHED)) {
            log.warn("重复评价被拦截：orderId={}, 已有评价={}, 当前状态={}",
                    order.getOrderId(), alreadyEvaluated, order.getStatus());
            throw new BusinessException(ResultCode.CONFLICT, EvaluationConstants.MSG_ALREADY_EVALUATED);
        }

        // ④ BR-6-01 状态前置断言：仅「已验收(5)」可评价（409）
        if (!isStatus(order.getStatus(), OrderConstants.STATUS_VERIFIED)) {
            throw new BusinessException(ResultCode.CONFLICT, EvaluationConstants.MSG_ORDER_NOT_VERIFIED);
        }

        // ⑤ BR-6-03 评分取值域兜底（@Min/@Max 已拦一次，这里再校验一次）
        int scoreService = requireScore(dto.getScoreService(), "服务态度");
        int scoreSpeed = requireScore(dto.getScoreSpeed(), "响应速度");
        int scoreQuality = requireScore(dto.getScoreQuality(), "维修质量");

        // ⑥ 文本入参规范化（第二层校验：纯空格、长度、图片张数）
        String content = normalizeContent(dto.getContent());
        String images = normalizeImages(dto.getImages());
        int anonymous = normalizeAnonymous(dto.getIsAnonymous());

        // ⑦ 被评价人取自工单（worker_id NOT NULL，状态 5 的工单必有受派维修人员）
        Long workerId = order.getWorkerId();
        if (workerId == null) {
            log.error("数据异常：状态为已验收的工单缺少维修人员，orderId={}", order.getOrderId());
            throw new BusinessException(ResultCode.CONFLICT, EvaluationConstants.MSG_WORKER_MISSING);
        }

        // ⑧ 综合评分：三项均值四舍五入，服务端计算，不接受前端上传
        int totalScore = EvaluationConverter.computeTotalScore(scoreService, scoreSpeed, scoreQuality);

        LocalDateTime now = LocalDateTime.now();

        // ⑨ BR-6-04 状态流转 5 → 6：条件更新 + 受影响行数校验（禁止跳级 + 并发乐观锁）
        LambdaUpdateWrapper<RepairOrder> orderUpdate = new LambdaUpdateWrapper<>();
        orderUpdate.eq(RepairOrder::getOrderId, order.getOrderId())
                // 前置状态固化在 WHERE 中：任何不在状态机内的迁移都不可能命中
                .eq(RepairOrder::getStatus, OrderConstants.STATUS_VERIFIED)
                .set(RepairOrder::getStatus, EvaluationConstants.TARGET_STATUS_FINISHED)
                .set(RepairOrder::getUpdateTime, now);
        if (repairOrderMapper.update(null, orderUpdate) != 1) {
            // 并发场景：本请求处理期间工单已被另一请求评价（状态已变为 6）
            log.warn("评价并发冲突：工单状态已被推进，orderId={}", order.getOrderId());
            throw new BusinessException(ResultCode.CONFLICT, EvaluationConstants.MSG_ALREADY_EVALUATED);
        }

        // ⑩ 评价落库（一单一评由 uk_order_id 唯一索引兜底，冲突转 409）
        RepairEvaluation evaluation = new RepairEvaluation();
        evaluation.setOrderId(order.getOrderId());
        evaluation.setResidentId(current.getUserId());
        evaluation.setWorkerId(workerId);
        evaluation.setScoreService(scoreService);
        evaluation.setScoreSpeed(scoreSpeed);
        evaluation.setScoreQuality(scoreQuality);
        evaluation.setTotalScore(totalScore);
        evaluation.setContent(content);
        evaluation.setImages(images);
        evaluation.setIsAnonymous(anonymous);
        try {
            repairEvaluationMapper.insert(evaluation);
        } catch (DuplicateKeyException e) {
            log.warn("评价唯一索引冲突（一单一评）：orderId={}", order.getOrderId());
            throw new BusinessException(ResultCode.CONFLICT, EvaluationConstants.MSG_ALREADY_EVALUATED);
        }

        order.setStatus(EvaluationConstants.TARGET_STATUS_FINISHED);
        order.setUpdateTime(now);

        log.info("居民 {} 提交评价：evaluationId={}, orderId={}, 综合评分={}（{}/{}/{}）, 状态 5 → 6，业务闭环",
                current.getUserId(), evaluation.getEvaluationId(), order.getOrderId(),
                totalScore, scoreService, scoreSpeed, scoreQuality);
        return EvaluationConverter.toCreateVO(evaluation, order);
    }

    // ==================================================================================
    // 二、E-02 评价分页列表
    // ==================================================================================

    /**
     * E-02 评价分页列表。
     *
     * <p>对应需求 FR-6-04（我的评价）、FR-6-05（管理员列表 + 星级/时间/类目/人员筛选）、
     * FR-6-07（维修人员查看本人所接工单收到的评价）；接口文档 4.7.2。</p>
     *
     * <p><b>数据范围（服务端强制，前端无法绕过）</b>：
     * 居民强制附加 {@code resident_id = 当前用户ID}；
     * 维修人员强制附加 {@code worker_id = 当前用户ID}；
     * 管理员不限定。三个角色都在同一接口上，由服务端分别注入范围条件，
     * 前端即使篡改请求也拿不到范围外的数据（NFR-S-03）。</p>
     *
     * <p><b>筛选参数的权限约束</b>：{@code workerId} 与 {@code categoryId} 为管理员专属，
     * 非管理员传入直接 <b>403</b>（接口文档 4.7.2 明确标注「仅管理员可用」）。</p>
     *
     * <p><b>类目筛选为何用子查询？</b>{@code repair_evaluation} 表本身没有类目列
     * （类目归属工单），若为了筛选而冗余一份 {@code category_id} 会造成数据不一致。
     * 因此使用 {@code order_id IN (SELECT order_id FROM repair_order WHERE category_id = ?)}
     * 的子查询形式，<b>不新增字段、不新增表</b>；MyBatis-Plus 的 {@code apply(..., {0})}
     * 使用预编译占位符绑定参数，不存在 SQL 注入风险。</p>
     *
     * <p><b>排序口径</b>：按评价时间 <b>降序</b>（最新的评价排最前），
     * 同一时间再按主键降序，保证分页结果稳定不跳行。</p>
     */
    @Override
    public PageResult<EvaluationVO> pageEvaluations(EvaluationQueryDTO query) {
        LoginUser current = UserContext.require();
        String role = current.getRole();
        Long userId = current.getUserId();
        EvaluationQueryDTO safeQuery = query == null ? new EvaluationQueryDTO() : query;

        // ---- ① 筛选参数权限校验（workerId / categoryId 仅管理员可用） ----
        if (safeQuery.getWorkerId() != null && !isAdmin(role)) {
            log.warn("非管理员越权按维修人员筛选评价被拦截：userId={}, role={}", userId, role);
            throw new BusinessException(ResultCode.FORBIDDEN, EvaluationConstants.MSG_WORKER_FILTER_FORBIDDEN);
        }
        if (safeQuery.getCategoryId() != null && !isAdmin(role)) {
            log.warn("非管理员越权按类目筛选评价被拦截：userId={}, role={}", userId, role);
            throw new BusinessException(ResultCode.FORBIDDEN, EvaluationConstants.MSG_CATEGORY_FILTER_FORBIDDEN);
        }

        // ---- ② 评分筛选值取值域校验（400，接口文档 4.7.2） ----
        checkScoreFilter(safeQuery.getTotalScore(), "综合评分筛选");
        checkScoreFilter(safeQuery.getMaxScore(), "评分上限筛选");

        // ---- ③ 分页参数保护：pageNum >= 1，1 <= pageSize <= 100（接口文档 2.5 节） ----
        int pageNum = (safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1)
                ? 1 : safeQuery.getPageNum();
        int pageSize = (safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1)
                ? EvaluationConstants.DEFAULT_PAGE_SIZE
                : Math.min(safeQuery.getPageSize(), EvaluationConstants.MAX_PAGE_SIZE);

        LambdaQueryWrapper<RepairEvaluation> wrapper = new LambdaQueryWrapper<>();

        // ---- ④ 角色数据范围（需求文档 3.3，服务端强制注入） ----
        if (isResident(role)) {
            wrapper.eq(RepairEvaluation::getResidentId, userId);
        } else if (isRepair(role)) {
            wrapper.eq(RepairEvaluation::getWorkerId, userId);
        }
        // 管理员：不附加范围条件，可查看全部评价

        // ---- ⑤ 通用筛选条件 ----
        wrapper.eq(safeQuery.getTotalScore() != null, RepairEvaluation::getTotalScore, safeQuery.getTotalScore());
        wrapper.le(safeQuery.getMaxScore() != null, RepairEvaluation::getTotalScore, safeQuery.getMaxScore());
        // 是否已回复：已回复 = reply_content 非空，未回复 = reply_content 为空
        if (safeQuery.getHasReply() != null) {
            if (Boolean.TRUE.equals(safeQuery.getHasReply())) {
                wrapper.isNotNull(RepairEvaluation::getReplyContent)
                        .ne(RepairEvaluation::getReplyContent, "");
            } else {
                wrapper.and(w -> w.isNull(RepairEvaluation::getReplyContent)
                        .or().eq(RepairEvaluation::getReplyContent, ""));
            }
        }
        // 维修人员筛选（已在上方校验过仅管理员可用）
        wrapper.eq(isAdmin(role) && safeQuery.getWorkerId() != null,
                RepairEvaluation::getWorkerId, safeQuery.getWorkerId());
        // 类目筛选（已在上方校验过仅管理员可用）：类目归属工单，故走子查询
        if (isAdmin(role) && safeQuery.getCategoryId() != null) {
            wrapper.apply("order_id IN (SELECT order_id FROM repair_order "
                            + "WHERE category_id = {0} AND deleted = 0)",
                    safeQuery.getCategoryId());
        }

        // 评价时间区间：起补 00:00:00，止补 23:59:59（只传日期时）
        LocalDateTime startTime = parseQueryTime(safeQuery.getStartTime(), false, "评价时间起");
        LocalDateTime endTime = parseQueryTime(safeQuery.getEndTime(), true, "评价时间止");
        wrapper.ge(startTime != null, RepairEvaluation::getCreateTime, startTime);
        wrapper.le(endTime != null, RepairEvaluation::getCreateTime, endTime);

        // ---- ⑥ 排序：评价时间降序 + 主键降序保证分页稳定 ----
        wrapper.orderByDesc(RepairEvaluation::getCreateTime)
                .orderByDesc(RepairEvaluation::getEvaluationId);

        Page<RepairEvaluation> page = repairEvaluationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // ---- ⑦ 批量装配关联数据（避免逐条查询导致 N+1） ----
        List<RepairEvaluation> records = page.getRecords();
        Map<Long, RepairOrder> orderMap = loadOrders(records);
        Map<Long, RepairCategory> categoryMap = loadCategoryMap();
        Map<Long, SysUser> userMap = loadUserMap(collectUserIds(records));

        return PageResult.of(page, evaluation -> {
            RepairOrder order = evaluation.getOrderId() == null ? null : orderMap.get(evaluation.getOrderId());
            String categoryName = order == null ? null : categoryDisplayName(categoryMap, order.getCategoryId());
            return EvaluationConverter.toEvaluationVO(
                    evaluation,
                    order == null ? null : order.getOrderNo(),
                    categoryName,
                    realName(userMap, evaluation.getResidentId()),
                    realName(userMap, evaluation.getWorkerId()),
                    // 传入当前查看者 ID，由转换器统一完成匿名脱敏（BR-6-06）
                    userId);
        });
    }

    // ==================================================================================
    // 三、E-03 评价详情
    // ==================================================================================

    /**
     * E-03 评价详情。
     *
     * <p>对应需求 FR-6-04；接口文档 4.7.3。</p>
     *
     * <p>返回结构 = E-02 列表元素（同一份 {@link EvaluationVO}）+ 工单摘要子对象
     * （{@code orderNo} / {@code title} / {@code categoryName} / {@code finishTime} / {@code totalCost}），
     * 其中工单完整信息仍由模块二 O-03 承载，本模块不重复实现一套工单详情。</p>
     *
     * <p><b>数据范围（服务端强制）</b>：居民仅可查看<b>本人提交</b>的评价、
     * 维修人员仅可查看<b>本人受派工单</b>收到的评价、管理员可查看全部；
     * 不符返回 <b>403</b>，评价不存在返回 <b>404</b>。</p>
     */
    @Override
    public EvaluationDetailVO getEvaluationDetail(Long evaluationId) {
        LoginUser current = UserContext.require();

        RepairEvaluation evaluation = selectExistingEvaluation(evaluationId);
        // 数据归属校验（与 E-02 的数据范围口径完全一致）
        assertViewPermission(evaluation, current);

        RepairOrder order = evaluation.getOrderId() == null
                ? null : repairOrderMapper.selectById(evaluation.getOrderId());
        Map<Long, RepairCategory> categoryMap = loadCategoryMap();
        String categoryName = order == null ? null : categoryDisplayName(categoryMap, order.getCategoryId());

        Set<Long> userIds = new LinkedHashSet<>();
        addIfNotNull(userIds, evaluation.getResidentId());
        addIfNotNull(userIds, evaluation.getWorkerId());
        Map<Long, SysUser> userMap = loadUserMap(userIds);

        EvaluationVO base = EvaluationConverter.toEvaluationVO(
                evaluation,
                order == null ? null : order.getOrderNo(),
                categoryName,
                realName(userMap, evaluation.getResidentId()),
                realName(userMap, evaluation.getWorkerId()),
                current.getUserId());

        return EvaluationConverter.toDetailVO(base, order, categoryName);
    }

    // ==================================================================================
    // 四、E-04 回复评价
    // ==================================================================================

    /**
     * E-04 回复评价。
     *
     * <p>对应需求 FR-6-06；BR-6-07「管理员回复一条评价只能回复一次，回复后不可修改」；
     * 接口文档 4.7.4。</p>
     *
     * <p><b>「只能回复一次」的落地方式</b>：不采用「先查后写」的两步判断（存在并发窗口），
     * 而是用<b>条件更新</b> {@code UPDATE repair_evaluation SET reply_content = ?, reply_time = ?
     * WHERE evaluation_id = ? AND reply_content IS NULL}，
     * 受影响行数为 0 即说明该评价已被回复，返回 409「该评价已回复，不可重复回复」。
     * 这样即使两个管理员同时点击回复，也只有一个能成功，
     * 且「回复后不可修改」由 {@code reply_content IS NULL} 这个前置条件从机制上保证。</p>
     *
     * <p><b>回复人信息</b>：{@code replyAdminId} / {@code replyAdminName} 取当前登录管理员
     * （数据表未设计回复人列，本模块不改表结构，详见 {@code EvaluationReplyVO} 的说明）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationReplyVO replyEvaluation(Long evaluationId, EvaluationReplyDTO dto) {
        LoginUser current = requireAdmin();

        RepairEvaluation evaluation = selectExistingEvaluation(evaluationId);

        // 回复内容规范化：必填 5~200 字（BR-6-07）
        String replyContent = normalizeReply(dto.getReplyContent());
        LocalDateTime now = LocalDateTime.now();

        // 条件更新：reply_content IS NULL 既是「只能回复一次」的判据，也是「回复后不可修改」的保障
        LambdaUpdateWrapper<RepairEvaluation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RepairEvaluation::getEvaluationId, evaluationId)
                .isNull(RepairEvaluation::getReplyContent)
                .set(RepairEvaluation::getReplyContent, replyContent)
                .set(RepairEvaluation::getReplyTime, now)
                .set(RepairEvaluation::getUpdateTime, now);
        if (repairEvaluationMapper.update(null, wrapper) != 1) {
            log.warn("重复回复评价被拦截：evaluationId={}, adminId={}", evaluationId, current.getUserId());
            throw new BusinessException(ResultCode.CONFLICT, EvaluationConstants.MSG_ALREADY_REPLIED);
        }

        evaluation.setReplyContent(replyContent);
        evaluation.setReplyTime(now);

        log.info("管理员 {} 回复评价：evaluationId={}", current.getUserId(), evaluationId);
        return EvaluationConverter.toReplyVO(evaluation, current.getUserId(), current.getRealName());
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
            log.warn("非居民越权调用评价提交接口被拦截：userId={}, role={}",
                    current.getUserId(), current.getRole());
            throw new BusinessException(ResultCode.FORBIDDEN, EvaluationConstants.MSG_ROLE_NOT_RESIDENT);
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
            log.warn("非管理员越权调用评价回复接口被拦截：userId={}, role={}",
                    current.getUserId(), current.getRole());
            throw new BusinessException(ResultCode.FORBIDDEN, EvaluationConstants.MSG_ROLE_NOT_ADMIN);
        }
        return current;
    }

    /**
     * 数据归属校验：工单必须由当前登录居民本人提交（BR-6-06 前置校验）。
     *
     * <p><b>这是本模块的核心安全边界</b> —— 即使用户篡改请求直接调用 E-01，
     * 也无法评价他人的工单。不通过抛 403，提示语明确说明「仅工单报修人可提交评价」。</p>
     */
    private void assertOrderOwner(RepairOrder order, LoginUser current) {
        if (!Objects.equals(order.getResidentId(), current.getUserId())) {
            log.warn("居民越权评价非本人提交的工单：userId={}, orderId={}, 实际报修人={}",
                    current.getUserId(), order.getOrderId(), order.getResidentId());
            throw new BusinessException(ResultCode.FORBIDDEN, EvaluationConstants.MSG_NOT_ORDER_OWNER);
        }
    }

    /**
     * 评价的读操作数据范围校验（E-03 使用，规则与 E-02 的列表范围完全一致）。
     *
     * <p>居民仅本人提交、维修人员仅本人受派、管理员不限；其余组合一律 403。</p>
     */
    private void assertViewPermission(RepairEvaluation evaluation, LoginUser current) {
        String role = current.getRole();
        Long userId = current.getUserId();
        if (isAdmin(role)) {
            return;
        }
        if (isRepair(role) && Objects.equals(evaluation.getWorkerId(), userId)) {
            return;
        }
        if (isResident(role) && Objects.equals(evaluation.getResidentId(), userId)) {
            return;
        }
        log.warn("越权查看评价详情被拦截：userId={}, role={}, evaluationId={}",
                userId, role, evaluation.getEvaluationId());
        throw new BusinessException(ResultCode.FORBIDDEN, EvaluationConstants.MSG_NO_PERMISSION_VIEW);
    }

    // ==================================================================================
    // 六、私有方法 —— 目标对象查询
    // ==================================================================================

    /** 按主键查询未删除的工单，不存在时抛出 404（{@code @TableLogic} 自动追加 deleted = 0） */
    private RepairOrder selectExistingOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "被评价工单ID不能为空");
        }
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, EvaluationConstants.MSG_ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 按主键查询未删除的评价，不存在时抛出 404 */
    private RepairEvaluation selectExistingEvaluation(Long evaluationId) {
        if (evaluationId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评价ID不能为空");
        }
        RepairEvaluation evaluation = repairEvaluationMapper.selectById(evaluationId);
        if (evaluation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, EvaluationConstants.MSG_EVALUATION_NOT_FOUND);
        }
        return evaluation;
    }

    // ==================================================================================
    // 七、私有方法 —— 字段规范化（第二层业务校验）
    //     说明：@Valid 已做第一层校验，这里做第二层（业务语义更精确、可处理纯空格）
    // ==================================================================================

    /**
     * 单项评分校验（BR-6-03）：必须为 1~5 的整数。
     *
     * @param score 评分值
     * @param label 维度中文名（服务态度 / 响应速度 / 维修质量），用于拼装精确的 400 提示
     * @return 校验通过的评分值
     */
    private int requireScore(Integer score, String label) {
        if (score == null || score < EvaluationConstants.SCORE_MIN || score > EvaluationConstants.SCORE_MAX) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    label + EvaluationConstants.MSG_SCORE_INVALID_SUFFIX);
        }
        return score;
    }

    /** 评分筛选值校验（E-02 专用，越界返回 400） */
    private void checkScoreFilter(Integer score, String label) {
        if (score != null
                && (score < EvaluationConstants.SCORE_MIN || score > EvaluationConstants.SCORE_MAX)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "：" + EvaluationConstants.MSG_SCORE_FILTER_INVALID);
        }
    }

    /**
     * 评价内容规范化：选填；一旦填写则 ≤200 字；空白串统一转为 {@code null}。
     *
     * <p>空白串转 {@code null} 的目的：避免「空字符串 vs NULL」两种语义并存，
     * 同时也防住了「用一串空格绕过长度校验」这类入参。</p>
     */
    private String normalizeContent(String content) {
        String value = content == null ? null : content.trim();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() > EvaluationConstants.CONTENT_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, EvaluationConstants.MSG_CONTENT_TOO_LONG);
        }
        return value;
    }

    /**
     * 评价图片规范化：最多 3 张、总长 ≤1000，返回「去空格 + 剔除空段」后的逗号拼接串。
     *
     * <p>剔除空段是为了避免 {@code "a.jpg,,b.jpg"} 这类写法在张数统计时分母失真
     * （按 {@code split(",")} 会数成 3 张，实际只有 2 张）。</p>
     */
    private String normalizeImages(String images) {
        String value = images == null ? null : images.trim();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() > EvaluationConstants.IMAGES_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, EvaluationConstants.MSG_IMAGES_TOO_LENGTH);
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String part : value.split(EvaluationConstants.IMAGES_SEPARATOR)) {
            String path = part.trim();
            if (!StringUtils.hasText(path)) {
                continue;
            }
            count++;
            if (builder.length() > 0) {
                builder.append(EvaluationConstants.IMAGES_SEPARATOR);
            }
            builder.append(path);
        }
        if (count > EvaluationConstants.IMAGES_MAX_COUNT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, EvaluationConstants.MSG_IMAGES_TOO_MANY);
        }
        return count == 0 ? null : builder.toString();
    }

    /** 匿名标识规范化：默认 0（实名）；仅允许 0 / 1 */
    private int normalizeAnonymous(Integer isAnonymous) {
        if (isAnonymous == null) {
            return EvaluationConstants.ANONYMOUS_NO;
        }
        if (isAnonymous != EvaluationConstants.ANONYMOUS_NO
                && isAnonymous != EvaluationConstants.ANONYMOUS_YES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, EvaluationConstants.MSG_ANONYMOUS_INVALID);
        }
        return isAnonymous;
    }

    /** 回复内容规范化：必填 5~200 字（BR-6-07） */
    private String normalizeReply(String replyContent) {
        String value = replyContent == null ? null : replyContent.trim();
        if (!StringUtils.hasText(value)
                || value.length() < EvaluationConstants.REPLY_MIN_LENGTH
                || value.length() > EvaluationConstants.REPLY_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, EvaluationConstants.MSG_REPLY_INVALID);
        }
        return value;
    }

    /**
     * 解析评价时间查询参数，兼容 {@code yyyy-MM-dd HH:mm:ss} 与 {@code yyyy-MM-dd} 两种写法。
     *
     * @param text       原始字符串
     * @param endOfDay   true 表示该参数是「止」（只传日期时补 23:59:59），false 表示「起」（补 00:00:00）
     * @param fieldLabel 字段中文名，用于拼装更精确的 400 提示
     * @return 解析后的时间；参数为空时返回 {@code null}
     */
    private LocalDateTime parseQueryTime(String text, boolean endOfDay, String fieldLabel) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            // 只传日期时按当日 00:00:00 / 23:59:59 处理，避免前端漏拼时分秒导致 400
            if (value.length() == 10) {
                LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldLabel + "：" + EvaluationConstants.MSG_TIME_FORMAT);
        }
    }

    // ==================================================================================
    // 八、私有方法 —— 关联数据批量装配（避免 N+1）
    // ==================================================================================

    /**
     * 批量装配评价所涉工单（用于取工单编号与类目 ID）。
     *
     * <p>为什么不直接在当前查询里 LEFT JOIN 工单表？
     * 因为 MyBatis-Plus 的 {@code LambdaQueryWrapper} 面向单表，
     * 而本模块需要复用其动态条件能力（多筛选 + 多角色范围 + 分页）。
     * 采用「主表分页 + 批量补装」的两步式写法，
     * 与模块二 ~ 模块五的装配方式保持一致，且总查询次数恒定（不随页大小增长）。</p>
     */
    private Map<Long, RepairOrder> loadOrders(Collection<RepairEvaluation> evaluations) {
        Set<Long> orderIds = new LinkedHashSet<>();
        for (RepairEvaluation item : evaluations) {
            addIfNotNull(orderIds, item.getOrderId());
        }
        Map<Long, RepairOrder> map = new HashMap<>();
        if (orderIds.isEmpty()) {
            return map;
        }
        for (RepairOrder order : repairOrderMapper.selectBatchIds(orderIds)) {
            map.put(order.getOrderId(), order);
        }
        return map;
    }

    /**
     * 一次性装载全部类目，构建「类目ID -> 类目实体」。
     *
     * <p>{@code repair_category} 是基础数据字典表（初始化仅 9 条，规模恒定在个位/十位数），
     * 一次全量取出即可同时完成「子类目 + 父类目名」的拼装，优于按 parentId 二次查询
     * （与模块二、模块五的做法一致）。</p>
     */
    private Map<Long, RepairCategory> loadCategoryMap() {
        Map<Long, RepairCategory> map = new HashMap<>();
        for (RepairCategory category : repairCategoryMapper.selectList(null)) {
            map.put(category.getCategoryId(), category);
        }
        return map;
    }

    /** 二级类目拼成「父类目 / 子类目」（接口文档 4.7.2 响应示例口径），一级类目直接返回本身 */
    private String categoryDisplayName(Map<Long, RepairCategory> categoryMap, Long categoryId) {
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

    /** 批量装载用户实体，供评价人 / 被评价人姓名装配使用 */
    private Map<Long, SysUser> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, SysUser> map = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(userIds)) {
            map.put(user.getUserId(), user);
        }
        return map;
    }

    /** 收集评价记录涉及的居民与维修人员 ID（用于批量装配姓名） */
    private Set<Long> collectUserIds(Collection<RepairEvaluation> evaluations) {
        Set<Long> userIds = new HashSet<>();
        for (RepairEvaluation item : evaluations) {
            addIfNotNull(userIds, item.getResidentId());
            addIfNotNull(userIds, item.getWorkerId());
        }
        return userIds;
    }

    /** 取用户真实姓名；ID 为空或用户已不存在时返回 null */
    private String realName(Map<Long, SysUser> userMap, Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMap.get(userId);
        return user == null ? null : user.getRealName();
    }

    /** 把非空 ID 收集进集合，避免把 null 传给 selectBatchIds 造成空指针 */
    private void addIfNotNull(Set<Long> target, Long id) {
        if (id != null) {
            target.add(id);
        }
    }

    // ==================================================================================
    // 九、私有方法 —— 角色与状态判定的安全封装
    // ==================================================================================

    /** 是否居民（大小写不敏感，空值安全） */
    private boolean isResident(String role) {
        return RoleEnum.RESIDENT.getCode().equalsIgnoreCase(role);
    }

    /** 是否维修人员 */
    private boolean isRepair(String role) {
        return RoleEnum.REPAIR.getCode().equalsIgnoreCase(role);
    }

    /** 是否系统管理员 */
    private boolean isAdmin(String role) {
        return RoleEnum.ADMIN.getCode().equalsIgnoreCase(role);
    }

    /** 状态码比较的安全封装，避免 Integer 拆箱空指针 */
    private boolean isStatus(Integer actual, int expected) {
        return actual != null && actual == expected;
    }
}
