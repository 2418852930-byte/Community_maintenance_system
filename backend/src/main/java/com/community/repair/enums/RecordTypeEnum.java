package com.community.repair.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 维修记录类型枚举（模块四：维修进度管理）。
 *
 * <p>对应接口文档 2.6 节「数据字典（4）维修记录类型 recordType」、
 * 数据库 V2.0 第 4 张表 {@code repair_record.record_type} 的列注释，
 * 以及接口文档 Q5 对「枚举表述不一致」的统一口径。</p>
 *
 * <pre>
 * | 值 | 名称     | 说明                                     | 写入方               |
 * | 1  | 接单     | 维修人员确认接单（进度时间线首节点）     | 模块三 D-08          |
 * | 2  | 到达现场 | 维修人员到达报修现场，开始维修           | 模块四 R-03          |
 * | 3  | 维修进度 | 常规进度说明，可多次追加                 | 模块四 R-02          |
 * | 4  | 更换配件 | 更换配件的进度说明（承载配件与费用）     | 模块四 R-02          |
 * | 5  | 完工     | 提交维修完成结果（维修结果说明）         | 模块四 R-04          |
 * | 6  | 转派退回 | 拒单 / 退回记录                          | 模块三 D-09          |
 * </pre>
 *
 * <p><b>关于「暂停 / 等待配件」为何没有独立枚举值</b>：接口文档 Q5 已明确，
 * 需求文档 5.4.4 表述的进度节点类型为「接单 / 到场 / 维修中 / 等待配件 / 维修完成」，
 * 而数据库 {@code record_type} 的持久化取值只有上述 6 类。
 * 二者统一口径为：<b>「等待配件 / 暂停」这类中间态以「维修进度（3）」+ 进度说明文本承载</b>，
 * 不新增记录类型、更不新增工单状态 —— 因此本枚举严格只有 6 个值。</p>
 *
 * <p><b>与 {@code DispatchConstants.RECORD_TYPE_*} 的关系</b>：模块三写入的
 * 「接单(1)」「转派退回(6)」两个常量已存在于 {@code DispatchConstants}，本枚举<b>只读引用</b>其取值口径，
 * 不再重复定义状态/类型数字，避免出现两份字典。</p>
 */
@Getter
public enum RecordTypeEnum {

    /** 1 接单：维修人员确认接单（由模块三 D-08 写入，本模块只读展示） */
    ACCEPT(1, "接单", false),

    /** 2 到达现场：维修人员到达现场并开始维修（由 R-03 开始维修写入） */
    ARRIVE(2, "到达现场", true),

    /** 3 维修进度：日常进度说明，可多次追加（由 R-02 写入）；「等待配件 / 暂停」也走本类型 */
    PROGRESS(3, "维修进度", true),

    /** 4 更换配件：更换配件的进度说明，承载 {@code partsUsed} 与 {@code cost}（由 R-02 写入） */
    PARTS(4, "更换配件", true),

    /** 5 完工：提交维修完成结果，承载维修结果说明（由 R-04 写入） */
    FINISH(5, "完工", true),

    /** 6 转派退回：拒单 / 退回记录（由模块三 D-09 写入，本模块只读展示） */
    TRANSFER_BACK(6, "转派退回", false);

    /** 记录类型码（入库值，对应 repair_record.record_type） */
    private final Integer code;

    /** 记录类型中文名（接口出参 recordTypeName，前端标签直接展示） */
    private final String name;

    /** 是否允许通过 R-02「追加维修进度」接口写入（用于入参取值域校验） */
    private final boolean appendable;

    RecordTypeEnum(Integer code, String name, boolean appendable) {
        this.code = code;
        this.name = name;
        this.appendable = appendable;
    }

    /**
     * 根据记录类型码获取枚举；非 1~6 的取值返回 null。
     *
     * @param code 记录类型码
     * @return 对应枚举，不存在返回 null
     */
    public static RecordTypeEnum ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 记录类型码 -> 中文名；取值非法时返回「未知」，保证前端不会显示 null。
     *
     * @param code 记录类型码
     * @return 记录类型中文名
     */
    public static String nameOf(Integer code) {
        RecordTypeEnum typeEnum = ofCode(code);
        return typeEnum == null ? "未知" : typeEnum.getName();
    }

    /**
     * 判断给定取值是否属于「R-02 追加进度」允许的记录类型（仅 2 / 3 / 4）。
     *
     * <p>1 接单、5 完工、6 转派退回分别由 D-08 / R-04 / D-09 专有接口写入，
     * 不允许通过 R-02 伪造，这是接口文档 4.5.2 入参取值域的直接落地。</p>
     *
     * @param code 记录类型码
     * @return true 表示可由 R-02 写入
     */
    public static boolean isAppendable(Integer code) {
        RecordTypeEnum typeEnum = ofCode(code);
        return typeEnum != null && typeEnum.appendable;
    }
}
