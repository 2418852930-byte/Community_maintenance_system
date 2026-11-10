package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 抢单池列表元素 VO（对应接口 D-06 的 data.list 元素）。
 *
 * <p>依据：接口文档 4.4.6；需求 FR-3-08、BR-3-07。</p>
 *
 * <p>字段严格对齐接口文档 4.4.6 的返回参数表，并额外补齐
 * {@code urgencyName} 派生字段，使前端列表无需二次翻译即可直接渲染。
 * 抢单池是维修人员的视图，因此<b>不返回</b>维修人员、费用等与派单后相关的字段。</p>
 */
@Data
public class GrabPoolItemVO implements Serializable {

    /** 工单 ID */
    private Long orderId;

    /** 工单编号 */
    private String orderNo;

    /** 报修标题 */
    private String title;

    /** 类目名称（含父类目，如「水电维修 / 水管漏水」） */
    private String categoryName;

    /** 上门维修地址 */
    private String address;

    /** 紧急程度 0 普通 / 1 紧急 */
    private Integer urgency;

    /** 紧急程度中文名 */
    private String urgencyName;

    /** 居民期望上门时间 */
    private LocalDateTime expectTime;

    /** 提交时间 */
    private LocalDateTime createTime;

    /** 现场照片路径，逗号分隔 */
    private String images;

    /** 故障详细描述 */
    private String description;
}
