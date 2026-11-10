package com.community.repair.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 工单分页查询条件（对应接口 O-02：GET /api/orders）。
 *
 * <p>依据：接口文档 4.3.2；需求 FR-2-04、FR-3-01、FR-3-11、FR-4-01、FR-5-01。</p>
 *
 * <p><b>「一个接口、多套视图」是本模块的设计要点</b>：本查询对象同时支撑</p>
 * <ul>
 *   <li>居民「我的报修」—— 数据范围自动锁定为本人（{@code residentId} 由服务端强制注入）；</li>
 *   <li>维修人员「我的工单」—— 数据范围自动锁定为当前受派工单（{@code workerId} 由服务端强制注入）；</li>
 *   <li>管理员「待审核工单池」—— 传 {@code status = 8}（BR-3-10 审核工作台入口）；</li>
 *   <li>管理员「待派单池」—— 传 {@code status = 0}，紧急工单置顶（BR-3-07）；</li>
 *   <li>管理员「全部工单总览」—— 不传状态，返回全部。</li>
 * </ul>
 *
 * <p>分页参数命名遵循接口文档 2.5 节约定：pageNum 从 1 开始，pageSize 最大 100。</p>
 */
@Data
public class OrderQueryDTO implements Serializable {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（Service 中做上限保护） */
    private Integer pageSize = 10;

    /**
     * 工单状态，取值 0~9（10 态字典）。
     * <p>支持<b>单值或多值</b>：单值如 {@code 8}；多值以英文逗号分隔，如 {@code "0,8,9"}。
     * 声明为 String 而非 Integer，是为了兼容「逗号分隔多值」这一接口约定；
     * Service 层解析时做范围校验，越界返回 400。</p>
     */
    private String status;

    /** 报修类目ID */
    private Long categoryId;

    /** 紧急程度：0 普通 / 1 紧急 */
    private Integer urgency;

    /** 关键字，模糊匹配 orderNo / title / address / contactName */
    private String keyword;

    /**
     * 报修居民ID，<b>仅管理员可用</b>。
     * <p>居民与维修人员若传入本参数，服务端直接返回 403，
     * 防止通过篡改查询条件越权查看他人数据（接口文档 4.3.2）。</p>
     */
    private Long residentId;

    /** 维修人员ID，<b>仅管理员可用</b>；越权传入同样返回 403 */
    private Long workerId;

    /** 提交时间起，格式 yyyy-MM-dd HH:mm:ss */
    private String startTime;

    /** 提交时间止，格式 yyyy-MM-dd HH:mm:ss */
    private String endTime;

    /** 排序字段（下划线命名），默认 create_time；待派单池可传 urgency */
    private String orderBy;

    /** 排序方向 asc / desc，默认 desc */
    private String orderDir = "desc";
}
