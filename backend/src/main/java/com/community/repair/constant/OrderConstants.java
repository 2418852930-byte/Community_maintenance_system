package com.community.repair.constant;

/**
 * 报修工单提交模块业务常量。
 *
 * <p>与已有的 {@link UserConstants}（模块零）、{@link CategoryConstants}（模块一）
 * 保持同样的组织方式：把需求文档中出现的魔法数字、长度限制与统一提示语集中定义，
 * 便于论文说明与后续维护。<b>不修改前两个常量类。</b></p>
 *
 * <p>对应需求：FR-2-01 ~ FR-2-09、BR-2-01 ~ BR-2-09。
 * 提示语逐字对齐《03-接口设计文档（最终版）》4.3 节各接口的「状态码」说明。</p>
 */
public final class OrderConstants {

    private OrderConstants() {
    }

    // ==================================================================================
    // 一、工单状态（repair_order.status，10 态字典；中文名统一由 OrderStatusEnum 提供）
    //    此处保留 int 常量，便于在实体赋值与条件构造中直接使用，避免频繁装箱拆箱。
    // ==================================================================================

    /** 0 待派单（审核通过后由下游模块三处理） */
    public static final int STATUS_PENDING_DISPATCH = 0;

    /** 1 已派单 */
    public static final int STATUS_DISPATCHED = 1;

    /** 2 已接单 */
    public static final int STATUS_ACCEPTED = 2;

    /** 3 维修中 */
    public static final int STATUS_REPAIRING = 3;

    /** 4 待验收（同时用于列表 verifyOverdue 派生计算，FR-5-06） */
    public static final int STATUS_PENDING_VERIFY = 4;

    /** 5 已验收 */
    public static final int STATUS_VERIFIED = 5;

    /** 6 已完成（终态） */
    public static final int STATUS_FINISHED = 6;

    /** 7 已撤销（终态） */
    public static final int STATUS_CANCELED = 7;

    /** 8 待审核 —— <b>工单起始状态</b>（BR-2-04 / BR-3-10） */
    public static final int STATUS_PENDING_AUDIT = 8;

    /** 9 已驳回（仅此状态可执行 O-06 修改后重新提交） */
    public static final int STATUS_REJECTED = 9;

    /** 状态码下界（接口文档 4.3.2：status 取值范围 0~9） */
    public static final int STATUS_MIN = 0;

    /** 状态码上界（接口文档 4.3.2：status 取值范围 0~9） */
    public static final int STATUS_MAX = 9;

    // ==================================================================================
    // 二、紧急程度（repair_order.urgency，2 级口径）
    // ==================================================================================

    /** 紧急程度：0 普通（默认级别） */
    public static final int URGENCY_NORMAL = 0;

    /** 紧急程度：1 紧急（待派单池置顶） */
    public static final int URGENCY_URGENT = 1;

    /** 允许的可修改状态：待审核 */
    public static final int[] MODIFIABLE_STATUSES = {STATUS_PENDING_AUDIT, STATUS_PENDING_DISPATCH};

    /** 允许的撤销状态：待审核 / 待派单 / 已驳回（BR-2-07） */
    public static final int[] CANCELABLE_STATUSES = {
            STATUS_PENDING_AUDIT, STATUS_PENDING_DISPATCH, STATUS_REJECTED};

    // ==================================================================================
    // 三、字段长度限制（逐项对应建表 SQL 的列宽与需求文档的长度约束）
    // ==================================================================================

    /** 报修标题长度下限（接口文档 4.3.1：2~100 字） */
    public static final int TITLE_MIN_LENGTH = 2;

    /** 报修标题长度上限（对应字段 repair_order.title VARCHAR(100)） */
    public static final int TITLE_MAX_LENGTH = 100;

    /** 故障描述长度下限（BR-2-03：10~500 字） */
    public static final int DESCRIPTION_MIN_LENGTH = 10;

    /** 故障描述长度上限（BR-2-03：10~500 字） */
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    /** 上门维修地址长度下限（接口文档 4.3.1：2~255 字） */
    public static final int ADDRESS_MIN_LENGTH = 2;

    /** 上门维修地址长度上限（对应字段 repair_order.address VARCHAR(255)） */
    public static final int ADDRESS_MAX_LENGTH = 255;

    /** 联系人姓名长度下限（接口文档 4.3.1：2~50 字） */
    public static final int CONTACT_NAME_MIN_LENGTH = 2;

    /** 联系人姓名长度上限（对应字段 repair_order.contact_name VARCHAR(50)） */
    public static final int CONTACT_NAME_MAX_LENGTH = 50;

    /** 现场照片张数上限（FR-2-07：最多 3 张） */
    public static final int IMAGES_MAX_COUNT = 3;

    /** 现场照片路径总长度上限（对应字段 repair_order.images VARCHAR(1000)） */
    public static final int IMAGES_MAX_LENGTH = 1000;

    /** 图片路径分隔符（多张以英文逗号分隔） */
    public static final String IMAGES_SEPARATOR = ",";

    /** 撤销原因长度下限（接口文档 4.3.5：5~200 字） */
    public static final int CANCEL_REASON_MIN_LENGTH = 5;

    /** 撤销原因长度上限（对应字段 repair_order.cancel_reason VARCHAR(255)，业务收紧为 200） */
    public static final int CANCEL_REASON_MAX_LENGTH = 200;

    /** 关键词模糊查询最大长度，防止超长入参拖慢查询 */
    public static final int KEYWORD_MAX_LENGTH = 50;

    // ==================================================================================
    // 四、工单编号（BR-2-05：BX + yyyyMMdd + 4 位当日流水号）
    // ==================================================================================

    /** 工单编号前缀 */
    public static final String ORDER_NO_PREFIX = "BX";

    /** 工单编号中的日期格式 */
    public static final String ORDER_NO_DATE_PATTERN = "yyyyMMdd";

    /** 当日流水号位数（不足补 0） */
    public static final int ORDER_NO_SEQ_LENGTH = 4;

    /** 当日流水号上限（4 位共 9999 条/天，超出即认为异常） */
    public static final int ORDER_NO_MAX_SEQ = 9999;

    // ==================================================================================
    // 五、派生计算口径
    // ==================================================================================

    /** 待验收超时阈值（小时）：状态为 4 且距完工超过该值时 verifyOverdue = true（FR-5-06） */
    public static final int VERIFY_OVERDUE_HOURS = 48;

    /** 分页每页条数上限（接口文档 2.5 节：最大 100） */
    public static final int MAX_PAGE_SIZE = 100;

    /** 分页每页条数默认值 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    // ==================================================================================
    // 六、统一提示语（逐字对齐接口文档 4.3 节状态码说明）
    // ==================================================================================

    /** 工单不存在（404） */
    public static final String MSG_ORDER_NOT_FOUND = "工单不存在";

    /** 访问他人工单（403，BR-2-06） */
    public static final String MSG_NO_PERMISSION_VIEW = "无权限查看该工单";

    /** 操作他人工单（403） */
    public static final String MSG_NO_PERMISSION_OPERATE = "无权限操作该工单";

    /** 类目不存在（404） */
    public static final String MSG_CATEGORY_NOT_FOUND = "类目不存在";

    /** 所选类目已停用（409，BR-1-05） */
    public static final String MSG_CATEGORY_DISABLED = "所选类目已停用，请重新选择";

    /** 状态不允许修改（409，O-04） */
    public static final String MSG_STATUS_NOT_MODIFIABLE = "当前状态的工单不可编辑";

    /** 状态为已驳回时引导使用重新提交接口（409，O-04） */
    public static final String MSG_STATUS_REJECTED_USE_RESUBMIT = "该工单为已驳回状态，请使用重新提交接口修改后提交";

    /** 状态不允许撤销（409，O-05，BR-2-07） */
    public static final String MSG_STATUS_NOT_CANCELABLE = "当前状态的工单不可撤销";

    /** 状态不允许重新提交（409，O-06，BR-2-09） */
    public static final String MSG_STATUS_NOT_RESUBMITTABLE = "仅「已驳回」状态的工单可修改后重新提交";

    /** 重新提交时未提交任何可修改字段（400，O-06） */
    public static final String MSG_RESUBMIT_EMPTY = "请至少修改一项内容后再提交";

    /** 紧急程度取值非法（400） */
    public static final String MSG_URGENCY_INVALID = "紧急程度取值只能是 0（普通）或 1（紧急）";

    /** 工单状态取值非法（400） */
    public static final String MSG_STATUS_INVALID = "工单状态取值超出 0~9 范围";

    /** 期望上门时间早于当前时间（400，BR-2-08） */
    public static final String MSG_EXPECT_TIME_PAST = "期望上门时间不能早于当前时间";

    /** 现场照片超出张数上限（400，FR-2-07） */
    public static final String MSG_IMAGES_TOO_MANY = "现场照片最多上传 3 张";

    /** 现场照片路径总长超限（400） */
    public static final String MSG_IMAGES_TOO_LENGTH = "现场照片路径总长度不能超过 1000 个字符";

    /** 非管理员越权按居民筛选（403，O-02） */
    public static final String MSG_RESIDENT_FILTER_FORBIDDEN = "仅管理员可按报修居民筛选工单";

    /** 非管理员越权按维修人员筛选（403，O-02） */
    public static final String MSG_WORKER_FILTER_FORBIDDEN = "仅管理员可按维修人员筛选工单";

    /** 工单编号生成失败（500，理论上仅在高并发极端场景出现） */
    public static final String MSG_ORDER_NO_FAIL = "工单编号生成失败，请稍后重试";

    /** 当日工单流水号已达上限（500） */
    public static final String MSG_ORDER_NO_LIMIT = "当日工单量已达上限，请联系管理员";

    /** 查询关键字过长（400） */
    public static final String MSG_KEYWORD_TOO_LONG = "查询关键字长度不能超过 50 个字符";

    /** 时间格式非法（400） */
    public static final String MSG_TIME_FORMAT = "时间格式错误，应为 yyyy-MM-dd HH:mm:ss";

    /** 不支持的排序字段（400） */
    public static final String MSG_SORT_FIELD_INVALID = "不支持的排序字段，可选：create_time、update_time、urgency、status、expect_time、total_cost";

    /** 非居民角色调用提交接口（403，兜底防御） */
    public static final String MSG_ROLE_NOT_RESIDENT = "仅居民可提交报修工单";
}
