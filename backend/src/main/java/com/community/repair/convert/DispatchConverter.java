package com.community.repair.convert;

import com.community.repair.entity.RepairDispatchLog;
import com.community.repair.entity.RepairOrder;
import com.community.repair.entity.SysUser;
import com.community.repair.enums.AuditResultEnum;
import com.community.repair.enums.DispatchOpResultEnum;
import com.community.repair.enums.DispatchTypeEnum;
import com.community.repair.enums.OrderStatusEnum;
import com.community.repair.enums.UrgencyEnum;
import com.community.repair.vo.DispatchRecordVO;
import com.community.repair.vo.GrabPoolItemVO;
import com.community.repair.vo.GrabPublishVO;
import com.community.repair.vo.OrderAcceptVO;
import com.community.repair.vo.OrderAuditVO;
import com.community.repair.vo.OrderDispatchVO;
import com.community.repair.vo.OrderGrabVO;
import com.community.repair.vo.OrderReassignVO;
import com.community.repair.vo.OrderRejectVO;
import com.community.repair.vo.WorkerWorkloadStatVO;
import com.community.repair.vo.WorkerWorkloadVO;

import java.time.LocalDateTime;

/**
 * 工单派单调度模块 实体 -> VO 转换器。
 *
 * <p>与已有的 {@code UserConverter}（模块零）、{@code CategoryConverter}（模块一）、
 * {@code OrderConverter}（模块二）保持<b>完全相同的组织方式</b>：
 * 静态工具类 + 私有构造，集中处理三件事：</p>
 * <ol>
 *   <li><b>字典翻译</b>：状态码、操作类型、操作结果、紧急程度统一走枚举，
 *       保证字典只有一处定义；</li>
 *   <li><b>关联名称注入</b>：维修人员名、操作人名、驳回人名等全部由调用方
 *       <b>批量查询后传入</b>，转换器内不查库，避免 N+1；</li>
 *   <li><b>空值收敛</b>：{@code OrderAuditVO} 在「审核通过」时把驳回四要素统一置 null，
 *       与数据库 V2.0「驳回三要素仅状态 9 时有值」的约束双保险。</li>
 * </ol>
 *
 * <p><b>状态中文名的翻译委托给模块二的 {@link OrderConverter}</b>，
 * 不再自行实现一套 {@code statusName}，避免出现两份状态字典。</p>
 */
public final class DispatchConverter {

    private DispatchConverter() {
    }

    // ==================================================================================
    // 一、审核环节（D-12）
    // ==================================================================================

    /**
     * 实体 -> 审核结果 VO。
     *
     * @param order        审核后的工单实体（状态与驳回快照已由 Service 更新到内存对象）
     * @param result       审核结果枚举
     * @param rejectorName 驳回管理员姓名；<b>审核通过时必须传 null</b>
     * @return 审核结果 VO
     */
    public static OrderAuditVO toAuditVO(RepairOrder order, AuditResultEnum result, String rejectorName) {
        if (order == null || result == null) {
            return null;
        }
        boolean passed = AuditResultEnum.PASS == result;
        OrderAuditVO vo = new OrderAuditVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setAuditResult(result.getCode());
        vo.setAuditResultName(result.getName());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        // 驳回四要素出口收敛：审核通过时一律为 null，不受实体残留值影响
        vo.setRejectReason(passed ? null : order.getRejectReason());
        vo.setRejectorId(passed ? null : order.getRejectorId());
        vo.setRejectorName(passed ? null : rejectorName);
        vo.setRejectTime(passed ? null : order.getRejectTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }

    // ==================================================================================
    // 二、派单链路（D-02 / D-04 / D-05 / D-07 / D-08 / D-09）
    // ==================================================================================

    /**
     * 实体 -> 派单结果 VO（D-02）。
     *
     * @param order      派单后的工单实体
     * @param workerName 受派维修人员姓名
     */
    public static OrderDispatchVO toDispatchVO(RepairOrder order, String workerName) {
        if (order == null) {
            return null;
        }
        OrderDispatchVO vo = new OrderDispatchVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setWorkerId(order.getWorkerId());
        vo.setWorkerName(workerName);
        vo.setDispatcherId(order.getDispatcherId());
        vo.setDispatchTime(order.getDispatchTime());
        return vo;
    }

    /**
     * 实体 -> 改派结果 VO（D-04）。
     *
     * @param order      改派后的工单实体（{@code workerId} 已指向新维修人员）
     * @param workerName 新维修人员姓名
     */
    public static OrderReassignVO toReassignVO(RepairOrder order, String workerName) {
        if (order == null) {
            return null;
        }
        OrderReassignVO vo = new OrderReassignVO();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getStatus());
        vo.setWorkerId(order.getWorkerId());
        vo.setWorkerName(workerName);
        vo.setDispatchTime(order.getDispatchTime());
        return vo;
    }

    /**
     * 实体 -> 发布抢单池结果 VO（D-05）。
     *
     * @param order       工单实体（状态保持 0 待派单，发布动作不改变状态）
     * @param publishTime 发布时间
     */
    public static GrabPublishVO toGrabPublishVO(RepairOrder order, LocalDateTime publishTime) {
        if (order == null) {
            return null;
        }
        GrabPublishVO vo = new GrabPublishVO();
        vo.setOrderId(order.getOrderId());
        vo.setGrabEnabled(Boolean.TRUE);
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setPublishTime(publishTime);
        return vo;
    }

    /**
     * 实体 -> 抢单池列表元素 VO（D-06）。
     *
     * @param order        工单实体
     * @param categoryName 类目名称（含父类目，如「水电维修 / 水管漏水」）
     */
    public static GrabPoolItemVO toGrabPoolItemVO(RepairOrder order, String categoryName) {
        if (order == null) {
            return null;
        }
        GrabPoolItemVO vo = new GrabPoolItemVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTitle(order.getTitle());
        vo.setCategoryName(categoryName);
        vo.setAddress(order.getAddress());
        vo.setUrgency(order.getUrgency());
        vo.setUrgencyName(UrgencyEnum.nameOf(order.getUrgency()));
        vo.setExpectTime(order.getExpectTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setImages(order.getImages());
        vo.setDescription(order.getDescription());
        return vo;
    }

    /** 实体 -> 抢单结果 VO（D-07）；抢单不涉及派单管理员，故不返回 dispatcherId */
    public static OrderGrabVO toGrabVO(RepairOrder order) {
        if (order == null) {
            return null;
        }
        OrderGrabVO vo = new OrderGrabVO();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setWorkerId(order.getWorkerId());
        vo.setDispatchTime(order.getDispatchTime());
        return vo;
    }

    /** 实体 -> 接单结果 VO（D-08） */
    public static OrderAcceptVO toAcceptVO(RepairOrder order, Long recordId) {
        if (order == null) {
            return null;
        }
        OrderAcceptVO vo = new OrderAcceptVO();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setAcceptTime(order.getAcceptTime());
        vo.setRecordId(recordId);
        return vo;
    }

    /** 实体 -> 拒单结果 VO（D-09）；拒单后受派人信息被清空，故 workerId 恒为 null */
    public static OrderRejectVO toRejectVO(RepairOrder order, Long recordId) {
        if (order == null) {
            return null;
        }
        OrderRejectVO vo = new OrderRejectVO();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setWorkerId(order.getWorkerId());
        vo.setRecordId(recordId);
        return vo;
    }

    // ==================================================================================
    // 三、派单记录（D-10）
    // ==================================================================================

    /**
     * 派单记录实体 -> VO（D-10）。
     *
     * @param log            派单记录实体
     * @param operatorName   操作人姓名
     * @param operatorRole   操作人角色编码（resident / repair / admin）
     * @param fromWorkerName 原受派维修人员姓名（非改派场景传 null）
     * @param toWorkerName   本次操作的维修人员姓名（无维修人员参与时传 null，如发布抢单池）
     */
    public static DispatchRecordVO toDispatchRecordVO(RepairDispatchLog log, String operatorName,
                                                      String operatorRole, String fromWorkerName,
                                                      String toWorkerName) {
        if (log == null) {
            return null;
        }
        DispatchRecordVO vo = new DispatchRecordVO();
        vo.setDispatchId(log.getDispatchId());
        vo.setOrderId(log.getOrderId());
        vo.setDispatchType(log.getDispatchType());
        vo.setDispatchTypeName(DispatchTypeEnum.nameOf(log.getDispatchType()));
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(operatorName);
        vo.setOperatorRole(operatorRole);
        vo.setFromWorkerId(log.getFromWorkerId());
        vo.setFromWorkerName(fromWorkerName);
        vo.setToWorkerId(log.getToWorkerId());
        vo.setToWorkerName(toWorkerName);
        vo.setOpResult(log.getOpResult());
        vo.setOpResultName(DispatchOpResultEnum.nameOf(log.getOpResult()));
        vo.setRemark(log.getRemark());
        vo.setDispatchTime(log.getDispatchTime());
        return vo;
    }

    // ==================================================================================
    // 四、维修人员负载视图（D-01）
    // ==================================================================================

    /**
     * 维修人员档案 + 聚合统计 -> 负载视图元素 VO（D-01）。
     *
     * @param worker 维修人员账号实体（sys_user）
     * @param stat   该人员的工单量聚合结果；<b>无任何工单时传 null</b>，三个指标补 0
     */
    public static WorkerWorkloadVO toWorkloadVO(SysUser worker, WorkerWorkloadStatVO stat) {
        if (worker == null) {
            return null;
        }
        WorkerWorkloadVO vo = new WorkerWorkloadVO();
        vo.setWorkerId(worker.getUserId());
        vo.setRealName(worker.getRealName());
        vo.setPhone(worker.getPhone());
        vo.setWorkerType(worker.getWorkerType());
        vo.setSkillDesc(worker.getSkillDesc());
        vo.setWorkStatus(worker.getWorkStatus());
        vo.setStatus(worker.getStatus());
        // 无在册工单的人员（如新入职维修人员）补 0，避免前端出现 null
        vo.setPendingCount(stat == null ? 0L : stat.getPendingCount());
        vo.setProcessingCount(stat == null ? 0L : stat.getProcessingCount());
        vo.setFinishedCount(stat == null ? 0L : stat.getFinishedCount());
        return vo;
    }

    // ==================================================================================
    // 五、字典翻译
    // ==================================================================================

    /** 工单状态码 -> 中文名（委托模块二 OrderConverter，保证全项目一份字典） */
    public static String statusName(Integer status) {
        return OrderConverter.statusName(status);
    }
}
