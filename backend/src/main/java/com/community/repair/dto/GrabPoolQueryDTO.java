package com.community.repair.dto;

import com.community.repair.constant.DispatchConstants;
import lombok.Data;

import java.io.Serializable;

/**
 * 抢单池列表查询条件（对应接口 D-06：GET /api/orders/grab-pool）。
 *
 * <p>依据：接口文档 4.4.6；需求 FR-3-08、BR-3-07。</p>
 *
 * <p>抢单池的集合口径为「{@code status = 0}（待派单）且已发布至抢单池且尚未被领取」，
 * 本对象只承载用户可见的筛选维度；「已发布」这一集合条件由服务端强制注入
 * （见 {@code DispatchConstants} 中关于抢单池判定依据的说明），前端无法绕过。</p>
 *
 * <p>分页参数命名遵循接口文档 2.5 节约定：pageNum 从 1 开始，pageSize 最大 100。</p>
 */
@Data
public class GrabPoolQueryDTO implements Serializable {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100（Service 中做上限保护） */
    private Integer pageSize = DispatchConstants.DEFAULT_PAGE_SIZE;

    /** 按报修类目筛选 */
    private Long categoryId;

    /** 按紧急程度筛选：0 普通 / 1 紧急 */
    private Integer urgency;

    /** 按报修地址模糊筛选 */
    private String addressKeyword;
}
