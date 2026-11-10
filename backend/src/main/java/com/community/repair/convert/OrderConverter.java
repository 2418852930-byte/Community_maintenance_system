package com.community.repair.convert;

import com.community.repair.constant.OrderConstants;
import com.community.repair.entity.RepairOrder;
import com.community.repair.enums.OrderStatusEnum;
import com.community.repair.enums.RoleEnum;
import com.community.repair.enums.UrgencyEnum;
import com.community.repair.vo.OrderCancelVO;
import com.community.repair.vo.OrderCreateVO;
import com.community.repair.vo.OrderDetailVO;
import com.community.repair.vo.OrderListVO;
import com.community.repair.vo.OrderOperateVO;
import com.community.repair.vo.OrderResubmitVO;
import com.community.repair.vo.OrderUpdateVO;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 工单实体 -> VO 转换器（与已有的 {@code UserConverter}、{@code CategoryConverter}
 * 保持同样的组织方式）。
 *
 * <p>集中处理四件事，避免在 Service 中散落重复代码：</p>
 * <ol>
 *   <li><b>状态码 / 紧急程度翻译</b>：统一走 {@link OrderStatusEnum} 与 {@link UrgencyEnum}，
 *       保证枚举字典只有一处定义；</li>
 *   <li><b>派生字段计算</b>：{@code verifyOverdue}（待验收超时）与 {@code evaluated}（是否已评价）；</li>
 *   <li><b>操作标识计算</b>：{@link #buildOperate} 按「角色 + 数据归属 + 工单状态」统一算出
 *       {@code canOperate}，是前端按钮显隐的唯一依据；</li>
 *   <li><b>关联名称注入</b>：类目名、居民名、维修人员名、派单人名、驳回人名均由调用方
 *       批量查询后传入，避免 N+1 查询（与模块一 {@code CategoryConverter} 传 parentName 一致）。</li>
 * </ol>
 */
public final class OrderConverter {

    private OrderConverter() {
    }

    // ==================================================================================
    // 一、出参转换
    // ==================================================================================

    /** 实体 -> 提交结果 VO（O-01） */
    public static OrderCreateVO toCreateVO(RepairOrder order) {
        if (order == null) {
            return null;
        }
        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    /**
     * 实体 -> 列表元素 VO（O-02）。
     *
     * @param order        工单实体
     * @param categoryName 类目名称（含父类目，如「水电维修 / 水管漏水」）
     * @param residentName 报修居民姓名
     * @param workerName   维修人员姓名，未派单传 null
     */
    public static OrderListVO toListVO(RepairOrder order, String categoryName,
                                       String residentName, String workerName) {
        if (order == null) {
            return null;
        }
        OrderListVO vo = new OrderListVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTitle(order.getTitle());
        vo.setCategoryId(order.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setResidentId(order.getResidentId());
        vo.setResidentName(residentName);
        vo.setAddress(order.getAddress());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setUrgency(order.getUrgency());
        vo.setUrgencyName(urgencyName(order.getUrgency()));
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setWorkerId(order.getWorkerId());
        vo.setWorkerName(workerName);
        vo.setExpectTime(order.getExpectTime());
        vo.setTotalCost(order.getTotalCost());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setVerifyOverdue(isVerifyOverdue(order));
        return vo;
    }

    /**
     * 实体 -> 详情 VO（O-03）。
     *
     * @param order          工单实体
     * @param categoryName   类目名称（含父类目）
     * @param residentName   报修居民姓名
     * @param workerName     维修人员姓名，未派单传 null
     * @param dispatcherName 派单管理员姓名，未派单传 null
     * @param rejectorName   驳回管理员姓名；<b>仅状态 9 时由调用方传入，其余状态传 null</b>
     * @param evaluated      是否已评价（{@code repair_evaluation} 存在记录）
     * @param canOperate     当前登录用户可执行操作标识
     */
    public static OrderDetailVO toDetailVO(RepairOrder order, String categoryName, String residentName,
                                           String workerName, String dispatcherName, String rejectorName,
                                           boolean evaluated, OrderOperateVO canOperate) {
        if (order == null) {
            return null;
        }
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTitle(order.getTitle());
        vo.setDescription(order.getDescription());
        vo.setImages(order.getImages());
        vo.setCategoryId(order.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setResidentId(order.getResidentId());
        vo.setResidentName(residentName);
        vo.setAddress(order.getAddress());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setUrgency(order.getUrgency());
        vo.setUrgencyName(urgencyName(order.getUrgency()));
        vo.setExpectTime(order.getExpectTime());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));

        // ---- 审核驳回四要素：仅状态 9 已驳回时对外暴露，其余状态一律置 null ----
        // 数据库层已保证 reject_* 仅在状态 9 时有值，这里再做一次出口收敛，双保险。
        boolean rejected = isStatus(order.getStatus(), OrderConstants.STATUS_REJECTED);
        vo.setRejectReason(rejected ? order.getRejectReason() : null);
        vo.setRejectorId(rejected ? order.getRejectorId() : null);
        vo.setRejectorName(rejected ? rejectorName : null);
        vo.setRejectTime(rejected ? order.getRejectTime() : null);

        vo.setWorkerId(order.getWorkerId());
        vo.setWorkerName(workerName);
        vo.setDispatcherId(order.getDispatcherId());
        vo.setDispatcherName(dispatcherName);
        vo.setDispatchTime(order.getDispatchTime());
        vo.setDispatchRemark(order.getDispatchRemark());
        vo.setAcceptTime(order.getAcceptTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setTotalCost(order.getTotalCost());
        vo.setVerifyTime(order.getVerifyTime());
        vo.setVerifyResult(order.getVerifyResult());
        vo.setVerifyRemark(order.getVerifyRemark());
        vo.setCancelReason(order.getCancelReason());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setVerifyOverdue(isVerifyOverdue(order));
        vo.setEvaluated(evaluated);
        vo.setCanOperate(canOperate);
        return vo;
    }

    /** 实体 -> 修改结果 VO（O-04） */
    public static OrderUpdateVO toUpdateVO(RepairOrder order) {
        if (order == null) {
            return null;
        }
        OrderUpdateVO vo = new OrderUpdateVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    /** 实体 -> 撤销结果 VO（O-05） */
    public static OrderCancelVO toCancelVO(RepairOrder order) {
        if (order == null) {
            return null;
        }
        OrderCancelVO vo = new OrderCancelVO();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setCancelReason(order.getCancelReason());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    /** 实体 -> 重新提交结果 VO（O-06）；驳回快照此时已清空，故 rejectReason / rejectTime 恒为 null */
    public static OrderResubmitVO toResubmitVO(RepairOrder order) {
        if (order == null) {
            return null;
        }
        OrderResubmitVO vo = new OrderResubmitVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setRejectReason(order.getRejectReason());
        vo.setRejectTime(order.getRejectTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    // ==================================================================================
    // 二、派生字段计算
    // ==================================================================================

    /**
     * 计算「待验收超时」标识（FR-5-06，BR-5-08）。
     *
     * <p>口径：仅当工单状态为 4（待验收）且距完工时间超过
     * {@link OrderConstants#VERIFY_OVERDUE_HOURS} 小时时为 {@code true}。
     * 若 {@code finishTime} 为空（理论上级联异常），退化为使用 {@code updateTime} 计算，
     * 避免出现「状态已是待验收却永远不超时」的判定盲区。</p>
     */
    public static boolean isVerifyOverdue(RepairOrder order) {
        if (order == null || !isStatus(order.getStatus(), OrderConstants.STATUS_PENDING_VERIFY)) {
            return false;
        }
        LocalDateTime start = order.getFinishTime() != null ? order.getFinishTime() : order.getUpdateTime();
        if (start == null) {
            return false;
        }
        return Duration.between(start, LocalDateTime.now()).toHours() > OrderConstants.VERIFY_OVERDUE_HOURS;
    }

    /**
     * 计算当前登录用户可执行的操作标识（{@code canOperate}）。
     *
     * <p>判定维度有三个，缺一不可：<b>当前登录角色</b>（{@code role}）、
     * <b>数据归属</b>（是否本人提交 / 是否本人受派）、<b>工单当前状态</b>。
     * 这一方法与 Service 层的强制校验使用<b>同一套状态常量</b>，
     * 保证「前端能看到的按钮」与「后端允许的接口」始终一致。</p>
     *
     * <p>注意：返回 {@code false} 只表示界面不展示按钮，
     * 若前端被绕过直接请求接口，Service 层仍会重新校验并抛 403 / 409。</p>
     *
     * @param order     工单实体
     * @param userId    当前登录用户 ID（来自 {@code UserContext}）
     * @param role      当前登录用户实时角色编码：resident / repair / admin
     * @param evaluated 是否已评价（影响 canEvaluate）
     * @return 操作标识对象
     */
    public static OrderOperateVO buildOperate(RepairOrder order, Long userId, String role, boolean evaluated) {
        OrderOperateVO vo = new OrderOperateVO();
        if (order == null || userId == null || role == null) {
            return vo;
        }

        int status = order.getStatus() == null ? -1 : order.getStatus();
        boolean isResident = RoleEnum.RESIDENT.getCode().equalsIgnoreCase(role);
        boolean isRepair = RoleEnum.REPAIR.getCode().equalsIgnoreCase(role);
        boolean isAdmin = RoleEnum.ADMIN.getCode().equalsIgnoreCase(role);

        // 数据归属：居民看本人提交的工单；维修人员看本人受派的工单
        boolean isOwner = order.getResidentId() != null && order.getResidentId().equals(userId);
        boolean isAssignedWorker = order.getWorkerId() != null && order.getWorkerId().equals(userId);

        // ---------- 本模块（模块二）消费的三项 ----------
        // O-04 修改：居民 + 本人 + 状态 8 待审核 / 0 待派单（已驳回请走 O-06）
        vo.setCanModify(isResident && isOwner
                && (status == OrderConstants.STATUS_PENDING_AUDIT
                || status == OrderConstants.STATUS_PENDING_DISPATCH));
        // O-06 重新提交：居民 + 本人 + 状态 9 已驳回
        vo.setCanResubmit(isResident && isOwner && status == OrderConstants.STATUS_REJECTED);
        // O-05 撤销：居民本人 或 管理员 + 状态 8 / 0 / 9（BR-2-07）
        vo.setCanCancel(((isResident && isOwner) || isAdmin)
                && (status == OrderConstants.STATUS_PENDING_AUDIT
                || status == OrderConstants.STATUS_PENDING_DISPATCH
                || status == OrderConstants.STATUS_REJECTED));

        // ---------- 下游模块（模块三派单调度）消费 ----------
        vo.setCanAudit(isAdmin && status == OrderConstants.STATUS_PENDING_AUDIT);
        vo.setCanDispatch(isAdmin && status == OrderConstants.STATUS_PENDING_DISPATCH);
        vo.setCanReassign(isAdmin && status == OrderConstants.STATUS_DISPATCHED);
        vo.setCanAccept(isRepair && isAssignedWorker && status == OrderConstants.STATUS_DISPATCHED);

        // ---------- 下游模块（模块四维修进度 / 模块五验收 / 模块六评价）消费 ----------
        vo.setCanStart(isRepair && isAssignedWorker && status == OrderConstants.STATUS_ACCEPTED);
        vo.setCanAddRecord(isRepair && isAssignedWorker && status == OrderConstants.STATUS_REPAIRING);
        vo.setCanFinish(isRepair && isAssignedWorker && status == OrderConstants.STATUS_REPAIRING);
        vo.setCanVerify(isResident && isOwner && status == OrderConstants.STATUS_PENDING_VERIFY);
        vo.setCanEvaluate(isResident && isOwner
                && status == OrderConstants.STATUS_VERIFIED && !evaluated);
        return vo;
    }

    // ==================================================================================
    // 三、字典翻译
    // ==================================================================================

    /** 工单状态码 -> 中文名（非法值返回「未知」） */
    public static String statusName(Integer status) {
        return OrderStatusEnum.nameOf(status);
    }

    /** 紧急程度码 -> 中文名（非法值返回「未知」） */
    public static String urgencyName(Integer urgency) {
        return UrgencyEnum.nameOf(urgency);
    }

    /** 状态码比较的安全封装，避免 Integer 拆箱空指针 */
    private static boolean isStatus(Integer actual, int expected) {
        return actual != null && actual == expected;
    }
}
