package com.community.repair.convert;

import com.community.repair.entity.RepairOrder;
import com.community.repair.entity.RepairVerifyLog;
import com.community.repair.enums.VerifyResultEnum;
import com.community.repair.enums.VerifyTypeEnum;
import com.community.repair.vo.AgentVerifyVO;
import com.community.repair.vo.VerifyRecordVO;
import com.community.repair.vo.VerifyResultVO;

/**
 * 工单验收管理模块 实体 -> VO 转换器（模块五）。
 *
 * <p>与已有的 {@code UserConverter}（模块零）、{@code CategoryConverter}（模块一）、
 * {@code OrderConverter}（模块二）、{@code DispatchConverter}（模块三）、
 * {@code ProgressConverter}（模块四）保持<b>完全相同的组织方式</b>：
 * 静态工具类 + 私有构造，集中处理两件事：</p>
 * <ol>
 *   <li><b>字典翻译</b>：验收结果与验收类型统一走 {@link VerifyResultEnum} /
 *       {@link VerifyTypeEnum}，保证字典只有一处定义；</li>
 *   <li><b>关联名称注入</b>：验收人姓名与角色由调用方<b>批量查询后传入</b>，
 *       转换器内不查库，避免 N+1（与模块四 {@code ProgressConverter} 传 workerName 一致）。</li>
 * </ol>
 *
 * <p><b>状态中文名的翻译委托给模块二的 {@link OrderConverter}</b>，
 * 不再自行实现一套 {@code statusName}，避免出现两份状态字典（与模块三、模块四做法一致）。</p>
 */
public final class VerifyConverter {

    private VerifyConverter() {
    }

    // ==================================================================================
    // 一、验收通过 / 驳回（V-02）
    // ==================================================================================

    /**
     * 工单 + 本轮验收信息 -> 验收结果 VO。
     *
     * @param order        状态已流转完成的工单实体（通过为 5，驳回为 3）
     * @param verifyRound  本轮验收轮次（已写入 {@code repair_verify_log.verify_round}）
     * @param verifyResult 验收结果：0 驳回 / 1 通过
     * @param verifyRemark 验收意见（通过时可空，驳回时必填）
     * @return 验收结果 VO
     */
    public static VerifyResultVO toVerifyResultVO(RepairOrder order, Integer verifyRound,
                                                 Integer verifyResult, String verifyRemark) {
        if (order == null) {
            return null;
        }
        // 是否通过：决定「验收时间是否对外暴露」与「是否解锁评价入口」两个分支
        boolean passed = VerifyResultEnum.isPass(verifyResult);

        VerifyResultVO vo = new VerifyResultVO();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setVerifyResult(verifyResult);
        vo.setVerifyResultName(VerifyResultEnum.nameOf(verifyResult));
        vo.setVerifyRound(verifyRound);
        // 驳回时工单未进入「已验收」，verify_time 不写入也不对外暴露（接口文档 4.6.2）
        vo.setVerifyTime(passed ? order.getVerifyTime() : null);
        vo.setVerifyRemark(verifyRemark);
        // 通过即解锁模块六评价入口；驳回时明确返回 false，前端据此不展示「去评价」
        vo.setCanEvaluate(passed);
        return vo;
    }

    // ==================================================================================
    // 二、管理员代验收（V-03）
    // ==================================================================================

    /**
     * 工单 + 本轮代验收信息 -> 代验收结果 VO。
     *
     * @param order        状态已流转为 5（已验收）的工单实体
     * @param verifyRound  本轮验收轮次
     * @param verifyRemark 代验收说明（BR-5-07 必填）
     * @param operatorName 操作管理员姓名；用户已不存在时传 null
     * @return 代验收结果 VO（{@code verifyResult} 与 {@code verifyType} 均为固定值）
     */
    public static AgentVerifyVO toAgentVerifyVO(RepairOrder order, Integer verifyRound,
                                               String verifyRemark, String operatorName) {
        if (order == null) {
            return null;
        }
        AgentVerifyVO vo = new AgentVerifyVO();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        // 本接口仅支持验收通过，结果固定为 1
        vo.setVerifyResult(VerifyResultEnum.PASS.getCode());
        vo.setVerifyRound(verifyRound);
        vo.setVerifyTime(order.getVerifyTime());
        vo.setVerifyRemark(verifyRemark);
        // BR-5-07：必须标识操作类型，固定为 2（管理员代验收）
        vo.setVerifyType(VerifyTypeEnum.ADMIN_AGENT.getCode());
        vo.setOperatorName(operatorName);
        return vo;
    }

    // ==================================================================================
    // 三、验收记录查询（V-04）
    // ==================================================================================

    /**
     * 验收记录实体 -> VO。
     *
     * @param log          验收记录实体（{@code repair_verify_log}）
     * @param verifierName 验收人姓名；用户已不存在时传 null
     * @param verifierRole 验收人角色编码（{@code resident} / {@code admin}）；
     *                     用户已不存在时传 null
     * @return 验收记录 VO
     */
    public static VerifyRecordVO toVerifyRecordVO(RepairVerifyLog log, String verifierName, String verifierRole) {
        if (log == null) {
            return null;
        }
        VerifyRecordVO vo = new VerifyRecordVO();
        vo.setVerifyId(log.getVerifyId());
        vo.setOrderId(log.getOrderId());
        vo.setVerifyRound(log.getVerifyRound());
        vo.setVerifyType(log.getVerifyType());
        vo.setVerifyTypeName(VerifyTypeEnum.nameOf(log.getVerifyType()));
        vo.setVerifyResult(log.getVerifyResult());
        vo.setVerifyResultName(VerifyResultEnum.nameOf(log.getVerifyResult()));
        vo.setVerifyRemark(log.getVerifyRemark());
        vo.setVerifierId(log.getVerifierId());
        vo.setVerifierName(verifierName);
        vo.setVerifierRole(verifierRole);
        vo.setVerifyTime(log.getVerifyTime());
        return vo;
    }

    // ==================================================================================
    // 四、私有工具方法
    // ==================================================================================

    /** 工单状态码 -> 中文名（委托模块二 OrderConverter，保证全项目一份字典） */
    public static String statusName(Integer status) {
        return OrderConverter.statusName(status);
    }
}
