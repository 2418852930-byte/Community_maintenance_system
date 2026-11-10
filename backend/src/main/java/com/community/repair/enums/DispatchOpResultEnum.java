package com.community.repair.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 派单操作结果枚举（对应数据库 V2.0 {@code repair_dispatch_log.op_result}）。
 *
 * <p>需求 FR-3-07 要求派单记录必须能展示「派单人、被派人、时间、<b>结果</b>」四要素，
 * 其中「结果」即由本枚举承载，接口出参字段为 {@code opResult} / {@code opResultName}。</p>
 *
 * <p>本模块的写入策略：<b>只有成功执行的动作才写入派单记录</b>（op_result 恒为 1 成功）。
 * 失败的动作（例如并发抢单被他人抢先、状态校验未通过）不产生审计行，
 * 因为失败的尝试并未改变工单的任何派单状态，写入反而会污染轨迹。</p>
 */
@Getter
public enum DispatchOpResultEnum {

    /** 1 成功 */
    SUCCESS(1, "成功"),

    /** 0 失败 */
    FAIL(0, "失败");

    /** 操作结果编码（入库值，对应 repair_dispatch_log.op_result） */
    private final Integer code;

    /** 操作结果中文名（接口出参 opResultName） */
    private final String name;

    DispatchOpResultEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据编码获取枚举；编码非法时返回 null。
     *
     * @param code 结果编码，1 成功 / 0 失败
     * @return 对应枚举，不存在返回 null
     */
    public static DispatchOpResultEnum ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 编码 -> 中文名；编码非法时返回「未知」。
     *
     * @param code 结果编码
     * @return 操作结果中文名
     */
    public static String nameOf(Integer code) {
        DispatchOpResultEnum resultEnum = ofCode(code);
        return resultEnum == null ? "未知" : resultEnum.getName();
    }
}
