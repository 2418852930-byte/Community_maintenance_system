package com.community.repair.convert;

import com.community.repair.constant.EvaluationConstants;
import com.community.repair.entity.RepairEvaluation;
import com.community.repair.entity.RepairOrder;
import com.community.repair.vo.EvaluationCreateVO;
import com.community.repair.vo.EvaluationDetailVO;
import com.community.repair.vo.EvaluationReplyVO;
import com.community.repair.vo.EvaluationVO;

import java.util.Objects;

/**
 * 服务评价模块 实体 -> VO 转换器（模块六）。
 *
 * <p>与已有的 {@code UserConverter}（模块零）、{@code CategoryConverter}（模块一）、
 * {@code OrderConverter}（模块二）、{@code DispatchConverter}（模块三）、
 * {@code ProgressConverter}（模块四）、{@code VerifyConverter}（模块五）保持
 * <b>完全相同的组织方式</b>：静态工具类 + 私有构造，集中处理三件事：</p>
 * <ol>
 *   <li><b>派生计算</b>：综合评分 {@link #computeTotalScore}（三项均值四舍五入）
 *       与匿名脱敏判定 {@link #shouldHideResident}；</li>
 *   <li><b>状态字典翻译</b>：工单状态中文名<b>委托模块二的
 *       {@link OrderConverter#statusName}</b>，不再自行实现一套，避免出现两份状态字典；</li>
 *   <li><b>关联名称注入</b>：类目名、居民名、维修人员名由调用方<b>批量查询后传入</b>，
 *       转换器内不查库，避免 N+1（与模块四、模块五做法一致）。</li>
 * </ol>
 */
public final class EvaluationConverter {

    private EvaluationConverter() {
    }

    // ==================================================================================
    // 一、出参转换
    // ==================================================================================

    /**
     * 评价实体 + 工单编号 -> 提交结果 VO（E-01）。
     *
     * @param evaluation 已完成落库的评价实体
     * @param order      状态已流转为 6（已完成）的工单实体
     * @return 提交结果 VO（{@code orderStatus} 固定为 6）
     */
    public static EvaluationCreateVO toCreateVO(RepairEvaluation evaluation, RepairOrder order) {
        if (evaluation == null || order == null) {
            return null;
        }
        EvaluationCreateVO vo = new EvaluationCreateVO();
        vo.setEvaluationId(evaluation.getEvaluationId());
        vo.setOrderId(evaluation.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setScoreService(evaluation.getScoreService());
        vo.setScoreSpeed(evaluation.getScoreSpeed());
        vo.setScoreQuality(evaluation.getScoreQuality());
        vo.setTotalScore(evaluation.getTotalScore());
        vo.setOrderStatus(order.getStatus());
        vo.setOrderStatusName(OrderConverter.statusName(order.getStatus()));
        vo.setCreateTime(evaluation.getCreateTime());
        return vo;
    }

    /**
     * 评价实体 -> 列表元素 VO（E-02）。
     *
     * <p>匿名脱敏在本方法内统一完成，是<b>全模块唯一的脱敏出口</b>：
     * 只要 {@code isAnonymous = 1} 且查看者不是评价人本人，
     * {@code residentId} 与 {@code residentName} 就会被遮蔽（BR-6-06）。
     * 这样 E-02 列表与 E-03 详情共用同一份脱敏逻辑，不会出现
     * 「列表匿名了、详情却泄露」的安全缺口。</p>
     *
     * @param evaluation   评价实体
     * @param orderNo      工单编号
     * @param categoryName 类目名称（含父类目）
     * @param residentName 评价居民姓名（原始值，脱敏由本方法处理）
     * @param workerName   被评价维修人员姓名
     * @param viewerId     当前登录用户 ID（判定「是否为评价人本人」）
     * @return 列表元素 VO
     */
    public static EvaluationVO toEvaluationVO(RepairEvaluation evaluation, String orderNo, String categoryName,
                                             String residentName, String workerName, Long viewerId) {
        if (evaluation == null) {
            return null;
        }
        boolean hideResident = shouldHideResident(evaluation.getIsAnonymous(), evaluation.getResidentId(), viewerId);

        EvaluationVO vo = new EvaluationVO();
        vo.setEvaluationId(evaluation.getEvaluationId());
        vo.setOrderId(evaluation.getOrderId());
        vo.setOrderNo(orderNo);
        vo.setCategoryName(categoryName);
        // 匿名且非本人：评价人信息整体遮蔽（BR-6-06）
        vo.setResidentId(hideResident ? null : evaluation.getResidentId());
        vo.setResidentName(hideResident ? EvaluationConstants.ANONYMOUS_DISPLAY_NAME : residentName);
        vo.setWorkerId(evaluation.getWorkerId());
        vo.setWorkerName(workerName);
        vo.setScoreService(evaluation.getScoreService());
        vo.setScoreSpeed(evaluation.getScoreSpeed());
        vo.setScoreQuality(evaluation.getScoreQuality());
        vo.setTotalScore(evaluation.getTotalScore());
        vo.setContent(evaluation.getContent());
        vo.setImages(evaluation.getImages());
        vo.setIsAnonymous(evaluation.getIsAnonymous());
        vo.setReplyContent(evaluation.getReplyContent());
        vo.setReplyTime(evaluation.getReplyTime());
        vo.setCreateTime(evaluation.getCreateTime());
        return vo;
    }

    /**
     * 评价列表元素 + 工单摘要 -> 评价详情 VO（E-03）。
     *
     * @param evaluation 已脱敏的列表元素 VO（由 {@link #toEvaluationVO} 产出）
     * @param order      对应的工单实体，可为 null（工单不存在时摘要整体为 null）
     * @param categoryName 类目名称（含父类目）
     * @return 详情 VO（含 order 子对象）
     */
    public static EvaluationDetailVO toDetailVO(EvaluationVO evaluation, RepairOrder order, String categoryName) {
        EvaluationDetailVO vo = new EvaluationDetailVO();
        vo.setEvaluation(evaluation);
        if (order == null) {
            vo.setOrder(null);
            return vo;
        }
        EvaluationDetailVO.OrderBrief brief = new EvaluationDetailVO.OrderBrief();
        brief.setOrderNo(order.getOrderNo());
        brief.setTitle(order.getTitle());
        brief.setCategoryName(categoryName);
        brief.setFinishTime(order.getFinishTime());
        brief.setTotalCost(order.getTotalCost());
        vo.setOrder(brief);
        return vo;
    }

    /**
     * 评价实体 + 操作管理员 -> 回复结果 VO（E-04）。
     *
     * @param evaluation  已完成回复落库的评价实体
     * @param adminId     本次执行回复操作的管理员 ID
     * @param adminName   本次执行回复操作的管理员姓名
     * @return 回复结果 VO
     */
    public static EvaluationReplyVO toReplyVO(RepairEvaluation evaluation, Long adminId, String adminName) {
        if (evaluation == null) {
            return null;
        }
        EvaluationReplyVO vo = new EvaluationReplyVO();
        vo.setEvaluationId(evaluation.getEvaluationId());
        vo.setReplyContent(evaluation.getReplyContent());
        vo.setReplyTime(evaluation.getReplyTime());
        vo.setReplyAdminId(adminId);
        vo.setReplyAdminName(adminName);
        return vo;
    }

    // ==================================================================================
    // 二、派生计算
    // ==================================================================================

    /**
     * 计算综合评分：三项评分求平均后<b>四舍五入</b>取整（接口文档 4.7 节开头、BR-6-03）。
     *
     * <p>实现要点：用 {@code / 3.0} 得到 {@code double} 再做 {@code Math.round}，
     * <b>不使用整数除法</b>（{@code (s1+s2+s3)/3} 会截断小数，例如 5+4+5=14 → 4，而正确结果是 5，
     * 与接口文档 4.7.1 响应示例（5,4,5 → totalScore = 5）不符）。</p>
     *
     * @param scoreService 服务态度评分
     * @param scoreSpeed   响应速度评分
     * @param scoreQuality 维修质量评分
     * @return 综合评分（1~5）；任一入参为 null 时返回 0，由调用方保证入参非空
     */
    public static int computeTotalScore(Integer scoreService, Integer scoreSpeed, Integer scoreQuality) {
        if (scoreService == null || scoreSpeed == null || scoreQuality == null) {
            return 0;
        }
        double average = (scoreService + scoreSpeed + scoreQuality) / (double) EvaluationConstants.SCORE_DIMENSION_COUNT;
        return (int) Math.round(average);
    }

    /**
     * 判定某条匿名评价是否需要向当前查看者隐藏评价人信息（BR-6-06）。
     *
     * <p>判定规则：<b>匿名 且 查看者不是评价人本人</b> → 需要隐藏。
     * 该单一条件同时覆盖了接口文档 4.7.2 的两句描述：</p>
     * <ul>
     *   <li>「匿名评价对<b>居民以外的角色</b>隐藏评价人信息」——维修人员与管理员必然不是评价人本人；</li>
     *   <li>「{@code residentId}（匿名时对<b>非本人</b>返回 null）」——评价人本人回看时不隐藏。</li>
     * </ul>
     *
     * @param isAnonymous 是否匿名（0 实名 / 1 匿名）
     * @param residentId  评价人 ID
     * @param viewerId    当前查看者 ID
     * @return true 表示需要隐藏评价人信息
     */
    public static boolean shouldHideResident(Integer isAnonymous, Long residentId, Long viewerId) {
        if (!Objects.equals(EvaluationConstants.ANONYMOUS_YES, isAnonymous)) {
            return false;
        }
        return !Objects.equals(residentId, viewerId);
    }
}
