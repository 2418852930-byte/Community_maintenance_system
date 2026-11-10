package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交报修工单返回结果 VO（对应接口 O-01 的 data 结构）。
 *
 * <p>依据：接口文档 4.3.1。{@code status} 字段<b>固定返回 8（待审核）</b>，
 * 与前端提交成功后的引导文案「提交成功，等待管理员审核」保持一致。</p>
 */
@Data
public class OrderCreateVO implements Serializable {

    /** 新建工单ID */
    private Long orderId;

    /** 工单编号，BX + yyyyMMdd + 4 位当日流水号 */
    private String orderNo;

    /** 工单状态，固定 8（待审核） */
    private Integer status;

    /** 状态中文名，固定「待审核」 */
    private String statusName;

    /** 提交时间 */
    private LocalDateTime createTime;
}
