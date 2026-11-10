package com.community.repair.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 服务评价表实体（对应《02-建表SQL（方案A）》第 5 张表 {@code repair_evaluation}）。
 *
 * <p>承载模块：<b>模块六 服务评价管理模块</b>（FR-6-01 ~ FR-6-08，接口 E-01 ~ E-04）。</p>
 *
 * <p><b>本表已存在于「方案A」建表脚本中，本模块不新增表、不修改表结构</b>
 * （字段与索引逐字对齐脚本第 216~241 行）。方案 A 兼容 MySQL 5.5~5.6，
 * {@code update_time} 由业务代码（{@code MyMetaObjectHandler}）维护，
 * 因此 {@code createTime} / {@code updateTime} 均标注 {@code @TableField(fill = ...)}。</p>
 *
 * <p><b>一单一评（BR-6-02）</b>：{@code order_id} 上建有唯一索引
 * {@code uk_order_id(order_id)}，这是「同一工单只能提交一条评价」的<b>数据库级兜底</b>；
 * Service 层先做「已存在评价」的业务前置断言（返回 409），
 * 并发场景则由该唯一索引抛 {@code DuplicateKeyException}，同样转 409。</p>
 *
 * <p><b>字段语义约定（与建表脚本注释逐字对齐）</b>：</p>
 * <ul>
 *   <li>{@code scoreService / scoreSpeed / scoreQuality}：三个维度的星级评分，均为 1~5 的整数（BR-6-03）；</li>
 *   <li>{@code totalScore}：综合评分，由三项求平均并四舍五入得出，<b>冗余存储</b>，
 *       便于评价列表排序与 {@code idx_worker_score(worker_id, total_score)} 上的人员均分统计；</li>
 *   <li>{@code content}：评价内容，选填（0~200 字）；</li>
 *   <li>{@code images}：评价图片，多张以英文逗号分隔，最多 3 张；</li>
 *   <li>{@code isAnonymous}：0 实名 / 1 匿名，匿名对非本人隐藏评价人信息（BR-6-06）；</li>
 *   <li>{@code replyContent / replyTime}：管理员一次性回复（BR-6-07，回复后不可修改）。</li>
 * </ul>
 *
 * <p><b>数据不可篡改（BR-6-05）</b>：本模块对 {@code repair_evaluation} 只提供
 * 「新增（E-01）」与「一次性回复（E-04）」两种写路径，
 * <b>不存在任何修改评分、修改内容或删除评价的接口</b>，保证评价数据的客观性。</p>
 */
@Data
@TableName("repair_evaluation")
public class RepairEvaluation implements Serializable {

    /** 评价ID，主键，自增 */
    @TableId(value = "evaluation_id", type = IdType.AUTO)
    private Long evaluationId;

    /** 被评价工单ID，关联 repair_order.order_id；<b>唯一索引 uk_order_id，一单一评</b> */
    private Long orderId;

    /** 评价居民ID，关联 sys_user.user_id（role = resident）；由 JWT 解析，不接受前端传入 */
    private Long residentId;

    /** 被评价维修人员ID，关联 sys_user.user_id（role = repair）；取自工单的 worker_id */
    private Long workerId;

    /** 服务态度评分，1~5 分 */
    private Integer scoreService;

    /** 响应速度评分，1~5 分 */
    private Integer scoreSpeed;

    /** 维修质量评分，1~5 分 */
    private Integer scoreQuality;

    /** 综合评分，1~5 分（三项均值四舍五入，由服务端计算后冗余存储） */
    private Integer totalScore;

    /** 评价内容，选填，0~200 字 */
    private String content;

    /** 评价图片路径，多张以英文逗号分隔，最多 3 张，总长 ≤ 1000 */
    private String images;

    /** 是否匿名：0 实名 / 1 匿名（BR-6-06） */
    private Integer isAnonymous;

    /** 管理员回复内容（BR-6-07：一条评价只能回复一次） */
    private String replyContent;

    /** 回复时间（业务时间，由业务代码显式赋值） */
    private LocalDateTime replyTime;

    /** 评价时间（创建时间），由 MyMetaObjectHandler 插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，由 MyMetaObjectHandler 插入/更新时自动填充（建表方案 A 要求业务代码维护） */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除。
     * <p>评价业务上不提供删除入口，保留该字段是为了与全库「逻辑删除」规范一致，
     * 并让下游统计查询可沿用统一的 {@code deleted = 0} 口径。</p>
     */
    @TableLogic
    @TableField("deleted")
    @JsonIgnore
    private Integer deleted;
}
