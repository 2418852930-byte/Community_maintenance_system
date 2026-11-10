package com.community.repair.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 工单状态枚举（对应接口文档 2.6 节「数据字典（2）工单状态 status」，
 * 与数据库 V2.0 附录 A「工单状态字典」、需求文档 V1.3 附录 A 完全一致）。
 *
 * <p><b>10 态字典</b>：</p>
 * <pre>
 * | 状态码 | 状态名称 | 说明                                                 | 可执行操作                 |
 * |   0    | 待派单   | 管理员审核通过，等待派单                             | 派单、发布抢单池、撤销     |
 * |   1    | 已派单   | 已指派维修人员，等待接单                             | 改派、接单、拒单           |
 * |   2    | 已接单   | 维修人员已确认接单                                   | 开始维修                   |
 * |   3    | 维修中   | 维修作业进行中                                       | 追加进度、提交维修完成     |
 * |   4    | 待验收   | 维修完成，等待居民验收                               | 验收通过、验收驳回、代验收 |
 * |   5    | 已验收   | 验收通过，等待评价                                   | 提交评价                   |
 * |   6    | 已完成   | 已评价，业务闭环结束                                 | 无                         |
 * |   7    | 已撤销   | 居民撤销的工单                                       | 无                         |
 * |   8    | 待审核   | 居民已提交（或驳回后修改重新提交），等待管理员审核   | 审核通过、审核驳回、撤销   |
 * |   9    | 已驳回   | 管理员审核不通过，等待居民修改后重新提交             | 修改内容后重新提交、撤销   |
 * </pre>
 *
 * <p><b>注意「编码顺序与业务顺序不一致」</b>：按业务时序「待审核(8)」是工单的起始状态，
 * 排在「待派单(0)」之前，故状态码数字大小<b>不代表</b>业务先后顺序。
 * 既有状态码 0~7 自 V1.0 起即已确定，新增状态顺延编码为 8、9，不重新排列。</p>
 *
 * <p><b>本模块（模块二）用到的状态</b>：</p>
 * <ul>
 *   <li>{@link #PENDING_AUDIT}（8）—— 提交后固定值，也是 O-06 重新提交后的目标值；</li>
 *   <li>{@link #PENDING_DISPATCH}（0）—— O-04 允许修改的状态之一（审核已通过）；</li>
 *   <li>{@link #REJECTED}（9）—— O-06 允许重新提交的唯一状态；</li>
 *   <li>{@link #CANCELED}（7）—— O-05 撤销后的目标值。</li>
 * </ul>
 * <p>其余状态由下游模块三 / 四 / 五流转，本模块只负责<b>如实展示</b>（工单列表全流程状态可见）。</p>
 */
@Getter
public enum OrderStatusEnum {

    /** 0 待派单：管理员审核通过，等待派单（下游模块三处理） */
    PENDING_DISPATCH(0, "待派单"),

    /** 1 已派单：已指派维修人员，等待接单（下游模块三处理） */
    DISPATCHED(1, "已派单"),

    /** 2 已接单：维修人员已确认接单（下游模块四处理） */
    ACCEPTED(2, "已接单"),

    /** 3 维修中：维修作业进行中（下游模块四处理） */
    REPAIRING(3, "维修中"),

    /** 4 待验收：维修完成，等待居民验收（下游模块五处理） */
    PENDING_VERIFY(4, "待验收"),

    /** 5 已验收：验收通过，等待评价（下游模块五/六处理） */
    VERIFIED(5, "已验收"),

    /** 6 已完成：已评价，业务闭环结束（终态） */
    FINISHED(6, "已完成"),

    /** 7 已撤销：居民撤销的工单（终态） */
    CANCELED(7, "已撤销"),

    /** 8 待审核：居民已提交，等待管理员审核；<b>工单起始状态</b> */
    PENDING_AUDIT(8, "待审核"),

    /** 9 已驳回：管理员审核不通过，等待居民修改后重新提交 */
    REJECTED(9, "已驳回");

    /** 状态码（入库值，对应 repair_order.status） */
    private final Integer code;

    /** 状态中文名（接口出参 statusName，前端标签直接展示） */
    private final String name;

    OrderStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据状态码获取枚举；状态码非法时返回 null。
     *
     * @param code 状态码，如 8
     * @return 对应枚举，不存在返回 null
     */
    public static OrderStatusEnum ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 状态码 -> 中文名；状态码非法时返回「未知」，保证前端不会显示 null。
     *
     * @param code 状态码
     * @return 状态中文名
     */
    public static String nameOf(Integer code) {
        OrderStatusEnum statusEnum = ofCode(code);
        return statusEnum == null ? "未知" : statusEnum.getName();
    }

    /**
     * 判断状态码是否合法（是否属于 10 态字典）。
     *
     * @param code 状态码
     * @return true 表示属于 0~9 之一
     */
    public static boolean isValid(Integer code) {
        return ofCode(code) != null;
    }
}
