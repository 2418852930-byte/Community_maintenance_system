package com.community.repair.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 派单操作类型枚举（对应接口文档 2.6 节「数据字典（4）派单操作类型 dispatchType」
 * 与数据库 V2.0 第 6 张表 {@code repair_dispatch_log.dispatch_type}）。
 *
 * <p>该字段是「工单派单调度模块」审计轨迹的唯一分类维度，
 * 每一次派单链路动作都独立成行写入 {@code repair_dispatch_log}（BR-3-08），
 * 管理员通过 D-10「派单记录查询」即可完整还原一张工单的派单链条。</p>
 *
 * <pre>
 * | 值 | 类型         | 操作人 | 状态影响      | 写入接口           |
 * |----|--------------|--------|---------------|--------------------|
 * | 1  | 派单         | admin  | 0 → 1         | D-02 / D-03        |
 * | 2  | 改派         | admin  | 1 → 1（人员变）| D-04              |
 * | 3  | 发布抢单池   | admin  | 0 → 0（不变） | D-05               |
 * | 4  | 抢单         | repair | 0 → 1         | D-07               |
 * | 5  | 接单         | repair | 1 → 2         | D-08               |
 * | 6  | 拒单         | repair | 1 → 0（回退） | D-09               |
 * </pre>
 *
 * <p><b>注意</b>：D-12「工单审核」（8 → 0 / 8 → 9）属于<b>审核链路</b>，
 * 不是派单动作，因此<b>不写入本表</b>（接口文档 4.4.12 与附录 A 明确约定：
 * 审核链路与派单链路在状态机上串联、在审计表上分离）。</p>
 */
@Getter
public enum DispatchTypeEnum {

    /** 1 派单：管理员将待派单工单指派给指定维修人员（D-02 / D-03） */
    DISPATCH(1, "派单"),

    /** 2 改派：管理员对已派单未接单的工单重新指派他人（D-04） */
    REASSIGN(2, "改派"),

    /** 3 发布抢单池：管理员将待派单工单发布为可抢单（D-05，状态不变） */
    PUBLISH_GRAB(3, "发布抢单池"),

    /** 4 抢单：维修人员从抢单池自行领取（D-07） */
    GRAB(4, "抢单"),

    /** 5 接单：维修人员确认接受分派（D-08） */
    ACCEPT(5, "接单"),

    /** 6 拒单：维修人员拒绝接单，工单回退至待派单（D-09） */
    REJECT(6, "拒单");

    /** 操作类型编码（入库值，对应 repair_dispatch_log.dispatch_type） */
    private final Integer code;

    /** 操作类型中文名（接口出参 dispatchTypeName） */
    private final String name;

    DispatchTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据编码获取枚举；编码非法时返回 null。
     *
     * @param code 操作类型编码，如 1
     * @return 对应枚举，不存在返回 null
     */
    public static DispatchTypeEnum ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 编码 -> 中文名；编码非法时返回「未知」，保证前端不会显示 null。
     *
     * @param code 操作类型编码
     * @return 操作类型中文名
     */
    public static String nameOf(Integer code) {
        DispatchTypeEnum typeEnum = ofCode(code);
        return typeEnum == null ? "未知" : typeEnum.getName();
    }

    /**
     * 判断编码是否合法（是否属于 1~6 六类操作）。
     *
     * @param code 操作类型编码
     * @return true 表示合法
     */
    public static boolean isValid(Integer code) {
        return ofCode(code) != null;
    }
}
