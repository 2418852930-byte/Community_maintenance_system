package com.community.repair.dto;

import com.community.repair.constant.DispatchConstants;
import lombok.Data;

import java.io.Serializable;

/**
 * 待派单池快捷查询条件（对应接口 D-11：GET /api/orders/pending-dispatch）。
 *
 * <p>依据：接口文档 4.4.11；需求 FR-3-01、BR-3-07。</p>
 *
 * <p>本接口是语义化快捷入口，<b>等价于 {@code GET /api/orders?status=0}</b>，
 * 默认按紧急程度降序、提交时间升序返回（紧急工单置顶，BR-3-07）。</p>
 *
 * <p><b>实现说明</b>：为保证与模块二 O-02 的列表口径、排序白名单、越权拦截、
 * 名称批量装配逻辑<b>完全一致且不重复开发</b>，Service 层会把本对象的条件
 * 转换为模块二的 {@code OrderQueryDTO}（强制 {@code status = 0}）后，
 * 直接复用模块二已实现的 {@code RepairOrderService#pageOrders} 完成查询，
 * 出参即为接口文档要求的「字段同 O-02 列表元素」。</p>
 */
@Data
public class PendingDispatchQueryDTO implements Serializable {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100 */
    private Integer pageSize = DispatchConstants.DEFAULT_PAGE_SIZE;

    /** 按报修类目筛选 */
    private Long categoryId;

    /** 按紧急程度筛选：0 普通 / 1 紧急 */
    private Integer urgency;

    /** 提交时间起，格式 yyyy-MM-dd HH:mm:ss（或 yyyy-MM-dd） */
    private String startTime;

    /** 提交时间止，格式 yyyy-MM-dd HH:mm:ss（或 yyyy-MM-dd） */
    private String endTime;

    /** 排序字段（下划线命名），默认按紧急程度置顶；可选 create_time/urgency 等白名单字段 */
    private String orderBy;

    /** 排序方向 asc / desc，默认 desc */
    private String orderDir = "desc";
}
