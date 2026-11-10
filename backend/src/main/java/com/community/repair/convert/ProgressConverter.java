package com.community.repair.convert;

import com.community.repair.entity.RepairOrder;
import com.community.repair.entity.RepairRecord;
import com.community.repair.enums.RecordTypeEnum;
import com.community.repair.vo.OrderDurationVO;
import com.community.repair.vo.OrderFinishVO;
import com.community.repair.vo.OrderRecordCreateVO;
import com.community.repair.vo.OrderRecordVO;
import com.community.repair.vo.OrderStartVO;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 维修进度管理模块 实体 -> VO 转换器（模块四）。
 *
 * <p>与已有的 {@code UserConverter}（模块零）、{@code CategoryConverter}（模块一）、
 * {@code OrderConverter}（模块二）、{@code DispatchConverter}（模块三）保持<b>完全相同的组织方式</b>：
 * 静态工具类 + 私有构造，集中处理三件事：</p>
 * <ol>
 *   <li><b>字典翻译</b>：记录类型与工单状态统一走枚举，保证字典只有一处定义；</li>
 *   <li><b>关联名称注入</b>：操作人姓名由调用方<b>批量查询后传入</b>，转换器内不查库，避免 N+1；</li>
 *   <li><b>耗时计算</b>：R-05 的五项耗时统一在本类计算，Service 只负责取数，
 *       保证「时间差 → 分钟数」的口径（含空值处理与四舍五入）全项目只有一份实现。</li>
 * </ol>
 *
 * <p><b>状态中文名的翻译委托给模块二的 {@link OrderConverter}</b>，
 * 不再自行实现一套 {@code statusName}，避免出现两份状态字典（与模块三做法一致）。</p>
 */
public final class ProgressConverter {

    private ProgressConverter() {
    }

    // ==================================================================================
    // 一、维修记录（R-01 / R-02）
    // ==================================================================================

    /**
     * 维修记录实体 -> VO。
     *
     * @param record     记录实体（repair_record）
     * @param workerName 操作人姓名；用户已不存在时传 null
     * @return 记录 VO
     */
    public static OrderRecordVO toRecordVO(RepairRecord record, String workerName) {
        if (record == null) {
            return null;
        }
        OrderRecordVO vo = new OrderRecordVO();
        vo.setRecordId(record.getRecordId());
        vo.setOrderId(record.getOrderId());
        vo.setWorkerId(record.getWorkerId());
        vo.setWorkerName(workerName);
        vo.setRecordType(record.getRecordType());
        vo.setRecordTypeName(RecordTypeEnum.nameOf(record.getRecordType()));
        vo.setProgressContent(record.getProgressContent());
        vo.setImages(record.getImages());
        vo.setPartsUsed(record.getPartsUsed());
        vo.setCost(record.getCost());
        vo.setRecordTime(record.getRecordTime());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    /**
     * 新增记录 + 工单 -> 追加进度结果 VO（R-02）。
     *
     * @param record 刚写入的记录实体（{@code recordId} 已回填）
     * @param order  工单实体（{@code status} 与 {@code totalCost} 为累加后的最新值）
     * @return 追加进度结果 VO
     */
    public static OrderRecordCreateVO toRecordCreateVO(RepairRecord record, RepairOrder order) {
        if (record == null || order == null) {
            return null;
        }
        OrderRecordCreateVO vo = new OrderRecordCreateVO();
        vo.setRecordId(record.getRecordId());
        vo.setOrderId(record.getOrderId());
        vo.setRecordType(record.getRecordType());
        vo.setRecordTypeName(RecordTypeEnum.nameOf(record.getRecordType()));
        vo.setRecordTime(record.getRecordTime());
        // 追加进度不改变状态，此处如实返回工单当前状态（2 或 3）
        vo.setOrderStatus(order.getStatus());
        vo.setTotalCost(order.getTotalCost());
        return vo;
    }

    // ==================================================================================
    // 二、开始维修（R-03）与提交完成（R-04）
    // ==================================================================================

    /**
     * 工单 + 新记录 ID -> 开始维修结果 VO（R-03）。
     *
     * @param order    状态已更新为 3（维修中）的工单实体
     * @param recordId 生成的「到达现场」记录 ID
     * @param now      本次变更时间
     */
    public static OrderStartVO toStartVO(RepairOrder order, Long recordId, LocalDateTime now) {
        if (order == null) {
            return null;
        }
        OrderStartVO vo = new OrderStartVO();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setRecordId(recordId);
        vo.setUpdateTime(now);
        return vo;
    }

    /**
     * 工单 + 新记录 ID -> 提交维修完成结果 VO（R-04）。
     *
     * @param order    状态已更新为 4（待验收）的工单实体（{@code finishTime} / {@code totalCost} 已是最新值）
     * @param recordId 生成的「完工」记录 ID
     */
    public static OrderFinishVO toFinishVO(RepairOrder order, Long recordId) {
        if (order == null) {
            return null;
        }
        OrderFinishVO vo = new OrderFinishVO();
        vo.setOrderId(order.getOrderId());
        vo.setStatus(order.getStatus());
        vo.setStatusName(statusName(order.getStatus()));
        vo.setFinishTime(order.getFinishTime());
        vo.setTotalCost(order.getTotalCost());
        vo.setRecordId(recordId);
        return vo;
    }

    // ==================================================================================
    // 三、耗时统计（R-05）
    // ==================================================================================

    /**
     * 工单 + 开始维修时间 -> 耗时统计 VO（R-05，BR-7-02 口径）。
     *
     * @param order     工单实体
     * @param startTime 开始维修时间（该工单首条「到达现场」记录的 {@code record_time}）；
     *                  尚无用该类记录时传 null
     * @return 耗时统计 VO，未流转到的节点其耗时项为 null
     */
    public static OrderDurationVO toDurationVO(RepairOrder order, LocalDateTime startTime) {
        if (order == null) {
            return null;
        }
        LocalDateTime createTime = order.getCreateTime();
        LocalDateTime dispatchTime = order.getDispatchTime();
        LocalDateTime acceptTime = order.getAcceptTime();
        LocalDateTime finishTime = order.getFinishTime();
        LocalDateTime verifyTime = order.getVerifyTime();

        OrderDurationVO vo = new OrderDurationVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setCreateTime(createTime);
        vo.setDispatchTime(dispatchTime);
        vo.setAcceptTime(acceptTime);
        vo.setStartTime(startTime);
        vo.setFinishTime(finishTime);
        vo.setVerifyTime(verifyTime);

        // 派单时长：派单时间 − 提交时间（待审核 8 / 已驳回 9 尚未派单，结果为 null）
        vo.setDispatchDurationMinutes(minutesBetween(createTime, dispatchTime));
        // 接单时长：接单时间 − 派单时间
        vo.setAcceptDurationMinutes(minutesBetween(dispatchTime, acceptTime));
        // 维修时长：完工时间 − 开始维修时间（起点取首条「到达现场」记录）
        vo.setRepairDurationMinutes(minutesBetween(startTime, finishTime));
        // 验收时长：验收时间 − 完工时间
        vo.setVerifyDurationMinutes(minutesBetween(finishTime, verifyTime));
        // 全流程时长：终点时间（验收优先，其次完工）− 提交时间
        LocalDateTime endTime = verifyTime != null ? verifyTime : finishTime;
        vo.setTotalDurationMinutes(minutesBetween(createTime, endTime));
        return vo;
    }

    // ==================================================================================
    // 四、私有工具方法
    // ==================================================================================

    /**
     * 计算两个时间点之间的分钟数（四舍五入到整分钟）。
     *
     * <p><b>为什么四舍五入而不是取整？</b>接口文档 4.5.5 的响应示例中，
     * 提交时间 09:12:30 到验收时间 19:00:00 相隔 587.5 分钟，
     * 展示值为 588，说明口径为「四舍五入到整分钟」。本方法即按该口径实现，
     * 保证 R-05 与统计分析模块 S-06 的数值可复现。</p>
     *
     * <p>任一时间为 null（节点尚未流转到）时返回 null，与接口文档
     * 「未流转到的节点时间为 null，对应时长也为 null」一致。</p>
     *
     * @param from 起始时间
     * @param to   结束时间
     * @return 分钟数；任一为空时返回 null
     */
    private static Long minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        long seconds = Duration.between(from, to).getSeconds();
        // 秒 -> 分钟的四舍五入：先转 double 再 Math.round，避免整数除法提前截断
        return Math.round(seconds / 60.0d);
    }

    /** 工单状态码 -> 中文名（委托模块二 OrderConverter，保证全项目一份字典） */
    public static String statusName(Integer status) {
        return OrderConverter.statusName(status);
    }
}
