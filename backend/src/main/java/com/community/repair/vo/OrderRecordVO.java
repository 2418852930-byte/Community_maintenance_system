package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维修进度记录 VO（对应接口 R-01 的 data 数组元素）。
 *
 * <p>依据：接口文档 4.5.1；需求 FR-4-07、BR-4-05。</p>
 *
 * <p>数据来源为 {@code repair_record} 表，是维修进度时间轴的基本单元，
 * 也是 R-02 追加成功后前端就地渲染新节点的数据结构。
 * 一条记录 = 一个进度节点，与工单<b>一一关联</b>（{@code orderId} 外键），
 * 记录本身<b>不可修改、不可删除</b>，从而保证维修过程可追溯（BR-4-05）。</p>
 *
 * <p>{@code workerName} 由 Service 批量装配后传入，转换器内不查库，避免 N+1。</p>
 */
@Data
public class OrderRecordVO implements Serializable {

    /** 记录 ID（repair_record.record_id） */
    private Long recordId;

    /** 所属工单 ID */
    private Long orderId;

    /** 操作人（维修人员）ID；来自模块三 D-08 的接单记录同样有值 */
    private Long workerId;

    /** 操作人姓名（批量装配，避免 N+1） */
    private String workerName;

    /** 记录类型：1 接单 / 2 到达现场 / 3 维修进度 / 4 更换配件 / 5 完工 / 6 转派退回 */
    private Integer recordType;

    /** 记录类型中文名（前端标签直接展示） */
    private String recordTypeName;

    /** 进度说明文字 */
    private String progressContent;

    /** 现场 / 完工图片路径，多张以英文逗号分隔 */
    private String images;

    /** 本次使用 / 更换的配件 */
    private String partsUsed;

    /** 本次费用（元） */
    private BigDecimal cost;

    /** 记录发生时间（业务时间，升序排列的时间轴依据） */
    private LocalDateTime recordTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
