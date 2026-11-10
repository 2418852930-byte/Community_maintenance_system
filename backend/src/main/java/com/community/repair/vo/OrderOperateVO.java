package com.community.repair.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 工单当前登录用户可执行操作标识（对应接口 O-03「工单详情」的 {@code canOperate} 子对象）。
 *
 * <p>依据：接口文档 4.3.3。<b>本对象是前端按钮显隐的唯一依据</b>，
 * 后端按「当前登录用户角色 + 工单当前状态 + 数据归属」统一计算后下发；
 * 前端不再自行拼装角色与状态的判断逻辑，避免前后端口径不一致。</p>
 *
 * <p>需要强调：这些标识只用于<b>界面显隐</b>。真正的权限与状态校验仍在
 * Service 层强制执行（NFR-S-03「禁止仅依赖前端隐藏菜单」），
 * 即便前端被绕过直接调接口，同样会被 {@code BusinessException} 拦下。</p>
 *
 * <p>本模块实际消费的标识为 {@code canModify} / {@code canResubmit} / {@code canCancel}；
 * 其余标识由下游模块三 / 四 / 五 / 六消费（例如 {@code canAudit} 对应 D-12 审核）。
 * 接口契约要求一次性返回完整对象，故此处完整定义 12 个字段。</p>
 */
@Data
public class OrderOperateVO implements Serializable {

    /** 是否可修改（居民 + 状态 8 或 0，对应 O-04） */
    private Boolean canModify = false;

    /** 是否可修改并重新提交（居民 + 状态 9，对应 O-06） */
    private Boolean canResubmit = false;

    /** 是否可撤销（居民 / 管理员 + 状态 8 / 0 / 9，对应 O-05） */
    private Boolean canCancel = false;

    /** 是否可审核（管理员 + 状态 8，对应 D-12，下游模块三） */
    private Boolean canAudit = false;

    /** 是否可派单（管理员 + 状态 0，下游模块三） */
    private Boolean canDispatch = false;

    /** 是否可改派（管理员 + 状态 1，下游模块三） */
    private Boolean canReassign = false;

    /** 是否可接单（维修人员 + 状态 1，下游模块三） */
    private Boolean canAccept = false;

    /** 是否可开始维修（维修人员 + 状态 2，下游模块四） */
    private Boolean canStart = false;

    /** 是否可追加进度（维修人员 + 状态 3，下游模块四） */
    private Boolean canAddRecord = false;

    /** 是否可提交完成（维修人员 + 状态 3，下游模块四） */
    private Boolean canFinish = false;

    /** 是否可验收（居民 + 状态 4，下游模块五） */
    private Boolean canVerify = false;

    /** 是否可评价（居民 + 状态 5 且未评价，下游模块六） */
    private Boolean canEvaluate = false;
}
