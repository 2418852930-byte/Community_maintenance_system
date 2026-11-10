package com.community.repair.service;

import com.community.repair.common.PageResult;
import com.community.repair.dto.AgentVerifyDTO;
import com.community.repair.dto.OrderVerifyDTO;
import com.community.repair.dto.PendingVerifyQueryDTO;
import com.community.repair.vo.AgentVerifyVO;
import com.community.repair.vo.OrderListVO;
import com.community.repair.vo.VerifyRecordVO;
import com.community.repair.vo.VerifyResultVO;

import java.util.List;

/**
 * 工单验收管理模块 业务接口（模块五，接口 V-01 ~ V-04）。
 *
 * <p>对应需求文档 5.5 节 FR-5-01 ~ FR-5-07、BR-5-01 ~ BR-5-08；
 * 接口文档 4.6 节。</p>
 *
 * <p><b>链路定位</b>：上游承接模块四「维修进度管理」流转的<b>待验收(4)</b>工单；
 * 验收通过后流转为<b>已验收(5)</b>，流入模块六「服务评价管理」；
 * 验收驳回则回退为<b>维修中(3)</b>，重新流入模块四返修。</p>
 *
 * <p><b>与相邻模块的边界（接口文档 4.6 节开头的明确约定，本模块严格遵守）</b>：</p>
 * <ul>
 *   <li>FR-5-01「待验收工单列表」虽也可由 {@code O-02 + status=4} 实现，
 *       但接口文档 4.6.1 已明确提供语义化快捷接口 <b>V-01</b> 承载
 *       {@code onlyOverdue} 与「按完工时间筛选」两项专属能力，本模块以其为准；</li>
 *   <li>FR-5-02「查看维修结果」复用模块二 <b>O-03</b>（工单详情）与模块四 <b>R-01</b>
 *       （维修进度时间线，含维修后照片），本模块<b>不重复建接口</b>；</li>
 *   <li>FR-5-06「超时提醒」在本模块内以 <b>V-01 的 {@code verifyOverdue} 字段</b>承载，
 *       不新增独立接口；</li>
 *   <li>本模块只承载 V-01 ~ V-04 四个接口，接口总数保持 <b>62 个</b>不变。</li>
 * </ul>
 */
public interface RepairVerifyService {

    /**
     * V-01 待验收工单快捷查询。
     *
     * <p>等价于 {@code GET /api/orders?status=4}，但额外支持
     * 「仅看超时未验收」（{@code onlyOverdue}）与「按完工时间区间筛选」。
     * 响应元素与 O-02 列表完全一致（{@link OrderListVO}），
     * 其中 {@code verifyOverdue} 为本接口的重点字段（FR-5-06，阈值 48 小时）。</p>
     *
     * <p><b>数据范围（服务端强制）</b>：居民仅返回本人提交的待验收工单；
     * 管理员返回全部；维修人员调用本接口返回 403（接口文档 4.6.1）。</p>
     *
     * @param query 查询条件（分页 / 类目 / 是否仅超时 / 完工时间区间）
     * @return 分页结果，无数据时 {@code list} 为空数组
     */
    PageResult<OrderListVO> pagePendingVerify(PendingVerifyQueryDTO query);

    /**
     * V-02 验收通过 / 驳回。
     *
     * <p>{@code verifyResult = 1}（通过）：状态 <b>4 → 5（已验收）</b>，
     * 写入 {@code verify_time} / {@code verify_result} / {@code verify_remark}，
     * 并解锁该工单的服务评价入口（返回 {@code canEvaluate = true}）；
     * {@code verifyResult = 0}（驳回）：状态 <b>4 → 3（维修中）</b>，
     * 原受派维修人员不变，工单重新流入模块四返修。</p>
     *
     * <p><b>校验链</b>：仅居民（403）→ 工单必须存在（404）→
     * 仅工单提交者本人可操作（403，BR-5-06）→ 状态必须为 4（409，BR-5-01；
     * 已完成验收时提示「该工单已完成验收」）→ 参数合法（400，驳回必填 5~200 字意见）。</p>
     *
     * <p><b>留痕</b>：无论通过还是驳回，都向 {@code repair_verify_log} 写入一行独立记录，
     * {@code verify_round} 自 1 起递增，{@code verify_type = 1}（BR-5-05）。</p>
     *
     * @param orderId 工单 ID
     * @param dto     验收结果与验收意见
     * @return 变更结果（含目标状态、轮次、是否解锁评价）
     */
    VerifyResultVO verifyOrder(Long orderId, OrderVerifyDTO dto);

    /**
     * V-03 管理员代验收。
     *
     * <p>待验收超时且居民长期未响应时，管理员代为验收。依据 BR-5-07，
     * 必须填写代验收说明，并以 {@code repair_verify_log.verify_type = 2} 标识操作类型。
     * <b>该接口仅支持验收通过，不支持代驳回。</b></p>
     *
     * <p><b>校验链</b>：仅管理员（403）→ 工单必须存在（404）→ 状态必须为 4（409）→
     * 必须已超过 48 小时待验收超时阈值（409，BR-5-08）→ 说明 5~200 字（400）。</p>
     *
     * @param orderId 工单 ID
     * @param dto     代验收说明
     * @return 变更结果（状态固定 5，验收类型固定 2）
     */
    AgentVerifyVO agentVerify(Long orderId, AgentVerifyDTO dto);

    /**
     * V-04 验收记录查询。
     *
     * <p>直查 {@code repair_verify_log}，按 {@code verify_round} <b>升序</b>
     * 返回该工单全部轮次的验收记录，体现 BR-5-05
     * 「多次驳回 → 重新维修 → 再次验收均独立留痕」。
     * {@code verify_type} 区分居民验收（1）与管理员代验收（2）。</p>
     *
     * <p><b>数据范围（服务端强制）</b>：居民仅可查看本人提交的工单、
     * 维修人员仅可查看本人受派的工单、管理员可查看全部；不符时返回 403。
     * 工单无验收记录时返回空数组而非 404。</p>
     *
     * @param orderId 工单 ID
     * @return 验收记录列表（按 verifyRound 升序）
     */
    List<VerifyRecordVO> listVerifyRecords(Long orderId);
}
