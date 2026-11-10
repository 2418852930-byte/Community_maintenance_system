package com.community.repair.constant;

/**
 * 工单派单调度模块业务常量（模块三）。
 *
 * <p>与已有的 {@link UserConstants}（模块零）、{@link CategoryConstants}（模块一）、
 * {@link OrderConstants}（模块二）保持完全一致的组织方式：把长度限制、数量上限、
 * 统计口径与统一提示语集中定义，便于论文说明与后续维护。
 * <b>本类不修改、不覆盖前三个常量类的任何内容。</b></p>
 *
 * <p>对应需求：FR-3-01 ~ FR-3-12、BR-3-01 ~ BR-3-10。
 * 提示语逐字对齐《03-接口设计文档（最终版）》4.4 节各接口的「状态码」说明。</p>
 *
 * <p><b>状态常量为何不在此重复定义？</b>工单状态是全局 10 态字典，
 * 模块二的 {@link OrderConstants} 已完整定义 {@code STATUS_*} 常量，
 * 本模块直接引用，避免出现「两份状态字典」的口径分裂风险。</p>
 */
public final class DispatchConstants {

    private DispatchConstants() {
    }

    // ==================================================================================
    // 一、审核环节（D-12，BR-3-01 / BR-3-09 / BR-3-10）
    // ==================================================================================

    /** 审核驳回原因长度下限（接口文档 4.4.12：5~200 字，BR-3-09） */
    public static final int REJECT_REASON_MIN_LENGTH = 5;

    /** 审核驳回原因长度上限（业务收紧为 200，对应 reject_reason VARCHAR(500)） */
    public static final int REJECT_REASON_MAX_LENGTH = 200;

    // ==================================================================================
    // 二、派单环节（D-02 ~ D-05）
    // ==================================================================================

    /** 派单备注 / 改派说明 / 发布说明长度上限（对应 dispatch_remark VARCHAR(255)） */
    public static final int DISPATCH_REMARK_MAX_LENGTH = 255;

    /** 批量派单单次最多工单数（接口文档 4.4.3） */
    public static final int BATCH_MAX_COUNT = 50;

    /** 接单备注长度上限（接口文档 4.4.8：≤ 500 字） */
    public static final int ACCEPT_REMARK_MAX_LENGTH = 500;

    /** 拒单原因长度下限（接口文档 4.4.9：5~200 字，BR-3-05） */
    public static final int REJECT_ORDER_REASON_MIN_LENGTH = 5;

    /** 拒单原因长度上限（对应 repair_dispatch_log.remark VARCHAR(255)，业务收紧为 200） */
    public static final int REJECT_ORDER_REASON_MAX_LENGTH = 200;

    /** 地址关键字模糊查询最大长度，防止超长入参拖慢查询 */
    public static final int ADDRESS_KEYWORD_MAX_LENGTH = 50;

    // ==================================================================================
    // 三、分页参数（与接口文档 2.5 节一致，与 OrderConstants 保持同口径）
    // ==================================================================================

    /** 分页每页条数上限 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 分页每页条数默认值 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    // ==================================================================================
    // 四、维修人员负载统计口径（D-01，对齐数据库 V2.0 附录 C2 与接口文档 4.4.1）
    //     注意：待审核(8) / 已驳回(9) / 已撤销(7) 尚未进入派单链路，不计入任何工作量指标
    // ==================================================================================

    /** 在途工单状态集合：已派单(1) / 已接单(2) / 维修中(3) / 待验收(4) */
    public static final int[] WORKLOAD_PENDING_STATUSES = {
            OrderConstants.STATUS_DISPATCHED,
            OrderConstants.STATUS_ACCEPTED,
            OrderConstants.STATUS_REPAIRING,
            OrderConstants.STATUS_PENDING_VERIFY};

    /** 处理中工单状态集合：已接单(2) / 维修中(3) */
    public static final int[] WORKLOAD_PROCESSING_STATUSES = {
            OrderConstants.STATUS_ACCEPTED,
            OrderConstants.STATUS_REPAIRING};

    /** 累计完成工单状态集合：已验收(5) / 已完成(6) */
    public static final int[] WORKLOAD_FINISHED_STATUSES = {
            OrderConstants.STATUS_VERIFIED,
            OrderConstants.STATUS_FINISHED};

    // ==================================================================================
    // 五、维修记录类型（D-08 接单、D-09 拒单需同步写入 repair_record，对齐数据库 V2.0 第 4 张表）
    // ==================================================================================

    /** 记录类型 1：接单（D-08 接单成功时自动写入，构成进度时间线的首个节点） */
    public static final int RECORD_TYPE_ACCEPT = 1;

    /** 记录类型 6：转派退回（D-09 拒单时写入，对应 BR-3-06） */
    public static final int RECORD_TYPE_TRANSFER_BACK = 6;

    /** 接单记录的默认进度文本（未填写接单备注时使用） */
    public static final String DEFAULT_ACCEPT_CONTENT = "维修人员已确认接单";

    // ==================================================================================
    // 六、统一提示语（逐字对齐接口文档 4.4 节状态码说明）
    // ==================================================================================

    // ---------- D-01 维修人员负载视图 ----------

    /** 查询成功（通用） */
    public static final String MSG_QUERY_SUCCESS = "查询成功";

    // ---------- D-02 派单 ----------

    /** 未指定维修人员（400） */
    public static final String MSG_WORKER_REQUIRED = "请选择维修人员";

    /** 维修人员不存在或角色不为 repair（404） */
    public static final String MSG_WORKER_NOT_FOUND = "维修人员不存在";

    /** 维修人员账号已停用（409，BR-3-03） */
    public static final String MSG_WORKER_DISABLED = "所选维修人员账号已停用，请重新选择";

    /** 状态不允许派单（409，BR-3-01） */
    public static final String MSG_STATUS_NOT_DISPATCHABLE = "该工单当前状态不允许派单";

    /** 工单尚未通过审核（409，D-02 状态码表专门列出的状态 8 情形） */
    public static final String MSG_NOT_AUDITED_YET = "该工单尚未通过审核，请先审核";

    /** 派单成功 */
    public static final String MSG_DISPATCH_SUCCESS = "派单成功";

    // ---------- D-03 批量派单 ----------

    /** 批量派单工单集合为空或超过上限（400） */
    public static final String MSG_BATCH_SIZE_INVALID = "批量派单的工单数量须为 1~50 条";

    /** 批量派单完成 */
    public static final String MSG_BATCH_DISPATCH_DONE = "批量派单完成";

    // ---------- D-04 改派 ----------

    /** 未指定新维修人员（400） */
    public static final String MSG_NEW_WORKER_REQUIRED = "请选择新的维修人员";

    /** 状态不允许改派（409，BR-3-02） */
    public static final String MSG_STATUS_NOT_REASSIGNABLE = "该工单当前状态不允许改派";

    /** 维修人员已接单，不允许改派（409，BR-3-02） */
    public static final String MSG_ALREADY_ACCEPTED = "该工单已被维修人员接单，不允许改派";

    /** 改派成功 */
    public static final String MSG_REASSIGN_SUCCESS = "改派成功";

    // ---------- D-05 发布抢单池 ----------

    /** 状态不允许发布（409，BR-3-01） */
    public static final String MSG_STATUS_NOT_PUBLISHABLE = "该工单当前状态不允许发布至抢单池";

    /** 该工单已在抢单池中（409，重复发布） */
    public static final String MSG_ALREADY_IN_GRAB_POOL = "该工单已在抢单池中";

    /** 发布成功 */
    public static final String MSG_PUBLISH_SUCCESS = "发布成功";

    // ---------- D-06 / D-07 抢单 ----------

    /** 工单未发布至抢单池或状态不允许抢单（409） */
    public static final String MSG_NOT_GRABBABLE = "该工单未发布至抢单池或状态不允许抢单";

    /** 并发抢单冲突，已被他人领取（409，BR-3-04） */
    public static final String MSG_GRABBED_BY_OTHER = "该工单已被其他维修人员领取";

    /** 抢单成功 */
    public static final String MSG_GRAB_SUCCESS = "抢单成功";

    // ---------- D-08 接单 ----------

    /** 状态不允许接单（409） */
    public static final String MSG_STATUS_NOT_ACCEPTABLE = "该工单当前状态不允许接单";

    /** 重复接单（409） */
    public static final String MSG_ALREADY_ACCEPTED_ONCE = "该工单已接单，请勿重复操作";

    /** 接单成功 */
    public static final String MSG_ACCEPT_SUCCESS = "接单成功";

    // ---------- D-09 拒单 ----------

    /** 拒单原因缺失或长度越界（400，BR-3-05） */
    public static final String MSG_REJECT_ORDER_REASON_INVALID = "拒单原因长度须为 5~200 个字符";

    /** 状态不允许拒单（409） */
    public static final String MSG_STATUS_NOT_REJECTABLE = "该工单当前状态不允许拒单";

    /** 拒单成功 */
    public static final String MSG_REJECT_ORDER_SUCCESS = "拒单成功";

    // ---------- D-10 派单记录查询 ----------

    /** 无权限访问该工单的派单记录（403） */
    public static final String MSG_NO_PERMISSION_DISPATCH_RECORD = "无权限访问该工单";

    // ---------- D-12 工单审核 ----------

    /** 审核结果取值非法（400） */
    public static final String MSG_AUDIT_RESULT_INVALID = "审核结果取值只能是 1（审核通过）或 0（审核驳回）";

    /** 审核驳回时未填驳回原因或长度越界（400，BR-3-09） */
    public static final String MSG_AUDIT_REJECT_REASON_INVALID = "审核驳回时必须填写驳回原因，且长度须为 5~200 个字符";

    /** 状态不允许审核（409，BR-3-01） */
    public static final String MSG_STATUS_NOT_AUDITABLE = "仅「待审核」状态的工单可执行审核";

    /** 审核通过成功 */
    public static final String MSG_AUDIT_PASS_SUCCESS = "审核通过，工单已进入待派单池";

    /** 审核驳回成功 */
    public static final String MSG_AUDIT_REJECT_SUCCESS = "审核驳回成功";

    // ---------- 通用兜底 ----------

    /** 非管理员角色越权调用派单调度类接口（403，兜底防御） */
    public static final String MSG_ROLE_NOT_ADMIN = "仅系统管理员可执行派单调度操作";

    /** 非维修人员角色调用抢单 / 接单 / 拒单接口（403，兜底防御） */
    public static final String MSG_ROLE_NOT_REPAIR = "仅维修人员可执行该操作";
}
