package com.community.repair.service;

import com.community.repair.common.PageResult;
import com.community.repair.dto.EvaluationCreateDTO;
import com.community.repair.dto.EvaluationQueryDTO;
import com.community.repair.dto.EvaluationReplyDTO;
import com.community.repair.vo.EvaluationCreateVO;
import com.community.repair.vo.EvaluationDetailVO;
import com.community.repair.vo.EvaluationReplyVO;
import com.community.repair.vo.EvaluationVO;

/**
 * 服务评价管理模块 业务接口（模块六，接口 E-01 ~ E-04）。
 *
 * <p>对应需求文档 5.6 节 FR-6-01 ~ FR-6-08、BR-6-01 ~ BR-6-07；
 * 接口文档 4.7 节。</p>
 *
 * <p><b>链路定位</b>：上游承接模块五「工单验收管理」流转的<b>已验收(5)</b>工单；
 * 评价提交后工单流转为<b>已完成(6)</b>，<b>业务链路彻底闭环</b>；
 * 评价数据同时开放给下游模块七「报修统计分析模块」调用（FR-6-08 由 S-07 实现）。</p>
 *
 * <p><b>与相邻模块的边界（接口文档 4.7 节开头的明确约定，本模块严格遵守）</b>：</p>
 * <ul>
 *   <li>FR-6-01「待评价工单列表」复用模块二 <b>O-02 + status=5</b>
 *       （{@code GET /api/orders?status=5}），本模块<b>不重复建接口</b>；
 *       O-03 出参的 {@code evaluated} 与 {@code canOperate.canEvaluate}
 *       已为评价入口提供显隐依据；</li>
 *   <li>FR-6-08「评价数据汇总」由模块七 <b>S-07</b>（{@code GET /api/statistics/evaluation}）
 *       统一聚合，本模块<b>不重复实现统计接口</b>，只保证评价明细可被下游读取；</li>
 *   <li>本模块只承载 E-01 ~ E-04 四个接口，接口总数由 62 增至 <b>66 个</b>。</li>
 * </ul>
 */
public interface RepairEvaluationService {

    /**
     * E-01 提交服务评价。
     *
     * <p>居民对<b>已验收(5)</b>的工单提交三项星级评分与文字评价；
     * 提交成功后工单状态由 5 置为 <b>6（已完成）</b>，业务链路彻底闭环（BR-6-04）。</p>
     *
     * <p><b>校验链</b>（按接口文档 4.7.1 状态码表逐条落地）：
     * 仅居民（403）→ 工单必须存在（404）→ 仅工单报修人本人可评价（403，BR-6-06）→
     * 状态必须为 5（409「工单尚未验收，无法评价」，BR-6-01）→
     * 该工单尚未被评价（409「该工单已评价，请勿重复提交」，BR-6-02）→
     * 评分 1~5、内容 ≤200 字、图片 ≤3 张（400，BR-6-03）。</p>
     *
     * <p><b>一单一评三重保障</b>：业务前置断言 + 条件更新
     * （{@code UPDATE repair_order ... WHERE status = 5} 受影响行数校验）+
     * 数据库唯一索引 {@code uk_order_id} 兜底。</p>
     *
     * @param dto 评价内容（工单 ID、三项评分、内容、图片、是否匿名）
     * @return 提交结果（含评价 ID、综合评分、工单变更后状态 6）
     */
    EvaluationCreateVO createEvaluation(EvaluationCreateDTO dto);

    /**
     * E-02 评价分页列表。
     *
     * <p><b>按角色自动限定数据范围</b>（服务端强制，前端无法绕过）：
     * 居民返回本人提交的评价（FR-6-04「我的评价」）；
     * 维修人员返回本人所接工单收到的评价（FR-6-07，匿名评价隐藏居民标识，BR-6-06）；
     * 管理员返回全部评价，支持按星级、时间、类目、维修人员筛选（FR-6-05）。</p>
     *
     * <p>{@code workerId} 与 {@code categoryId} 为管理员专属筛选维度，
     * 非管理员传入返回 403；工单无评价时 {@code list} 为空数组。</p>
     *
     * @param query 查询条件（分页 / 评分 / 是否已回复 / 时间区间 / 维修人员 / 类目）
     * @return 分页结果，无数据时 {@code list} 为空数组
     */
    PageResult<EvaluationVO> pageEvaluations(EvaluationQueryDTO query);

    /**
     * E-03 评价详情。
     *
     * <p>按 ID 查询单条评价的完整信息，并附加对应工单的摘要
     * （{@code orderNo} / {@code title} / {@code categoryName} / {@code finishTime} / {@code totalCost}）。</p>
     *
     * <p><b>数据范围（服务端强制）</b>：居民仅可查看本人评价、
     * 维修人员仅可查看本人相关评价（{@code worker_id = 当前用户}）、管理员可查看全部；
     * 不符时返回 403，评价不存在返回 404。</p>
     *
     * @param evaluationId 评价 ID
     * @return 评价详情（含工单摘要）
     */
    EvaluationDetailVO getEvaluationDetail(Long evaluationId);

    /**
     * E-04 回复评价。
     *
     * <p>管理员对评价进行文字回复（5~200 字）。依据 BR-6-07，
     * <b>一条评价只能回复一次，回复后不可修改</b>；重复回复返回 409
     * 「该评价已回复，不可重复回复」。</p>
     *
     * <p><b>校验链</b>：仅管理员（403）→ 评价必须存在（404）→
     * 该评价尚未回复（409）→ 回复内容 5~200 字（400）。</p>
     *
     * @param evaluationId 评价 ID
     * @param dto          回复内容
     * @return 回复结果（含回复时间与本次操作管理员信息）
     */
    EvaluationReplyVO replyEvaluation(Long evaluationId, EvaluationReplyDTO dto);
}
