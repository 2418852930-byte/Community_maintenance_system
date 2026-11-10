package com.community.repair.service;

import com.community.repair.dto.OrderFinishDTO;
import com.community.repair.dto.OrderRecordCreateDTO;
import com.community.repair.dto.OrderStartDTO;
import com.community.repair.vo.OrderDurationVO;
import com.community.repair.vo.OrderFinishVO;
import com.community.repair.vo.OrderRecordCreateVO;
import com.community.repair.vo.OrderRecordVO;
import com.community.repair.vo.OrderStartVO;

import java.util.List;

/**
 * 维修进度管理模块 业务接口（模块四，接口 R-01 ~ R-05）。
 *
 * <p>对应需求文档 5.4 节 FR-4-01 ~ FR-4-08、BR-4-01 ~ BR-4-06；
 * 接口文档 4.5 节。</p>
 *
 * <p><b>链路定位</b>：上游承接模块三「工单派单调度」分配的<b>已派单(1)</b>工单；
 * 维修完成后流转为<b>待验收(4)</b>，流入模块五「工单验收管理」。</p>
 *
 * <p><b>与相邻模块的边界（接口文档 4.5 节开头的明确约定，本模块严格遵守）</b>：</p>
 * <ul>
 *   <li>FR-4-01「我的工单列表」由模块二 <b>O-02</b>（{@code GET /api/orders}）按维修人员
 *       数据范围实现（服务端自动注入 {@code worker_id = 当前用户}），本模块<b>不重复建接口</b>；</li>
 *   <li>FR-4-02「接单」由模块三 <b>D-08</b> 实现，本模块<b>不重复建接口</b>；</li>
 *   <li>本模块只承载 R-01 ~ R-05 五个接口。</li>
 * </ul>
 */
public interface RepairProgressService {

    /**
     * R-01 进度时间线查询。
     *
     * <p>按 {@code record_time} 升序返回工单的全部进度节点，构成完整进度时间轴。</p>
     *
     * <p><b>数据范围（服务端强制）</b>：居民仅可查看本人提交的工单，
     * 维修人员仅可查看本人受派的工单，管理员可查看全部；不符时返回 403。</p>
     *
     * @param orderId    工单 ID
     * @param recordType 记录类型筛选，1~6；为 null 表示不筛选
     * @return 记录列表（无记录时返回空数组，不返回 404）
     */
    List<OrderRecordVO> listRecords(Long orderId, Integer recordType);

    /**
     * R-02 追加维修进度。
     *
     * <p>维修人员追加「到达现场(2) / 维修进度(3) / 更换配件(4)」三类记录，
     * 可携带照片、配件与本次费用；本次费用自动累加至工单 {@code total_cost}。</p>
     *
     * <p><b>校验链</b>：BR-4-01 仅本人受派工单（403）→ 状态须为 2 或 3（409）
     * → 记录类型须为 2/3/4、进度说明 2~200 字、照片 ≤ 3 张、费用非负（400）。</p>
     *
     * @param orderId 工单 ID
     * @param dto     追加进度入参
     * @return 新增记录结果（含工单累计费用）
     */
    OrderRecordCreateVO appendRecord(Long orderId, OrderRecordCreateDTO dto);

    /**
     * R-03 开始维修。
     *
     * <p>状态由 2（已接单）置为 3（维修中），并自动生成一条「到达现场」记录
     * （{@code record_type = 2}），其记录时间即 BR-7-02「维修时长」的起点。</p>
     *
     * <p><b>禁止跳级</b>：仅状态 2 可调用，其余状态一律 409「请按顺序更新进度」——
     * 「已派单(1)」必须先经模块三 D-08 接单，不允许直接开始维修。</p>
     *
     * @param orderId 工单 ID
     * @param dto     到场说明（可选）
     * @return 变更结果（含新记录 ID）
     */
    OrderStartVO startRepair(Long orderId, OrderStartDTO dto);

    /**
     * R-04 提交维修完成。
     *
     * <p>状态由 3（维修中）置为 4（待验收），记录 {@code finish_time}，
     * 并生成一条「完工」记录（{@code record_type = 5}）。
     * 维修结果说明必填且 10~300 字（BR-4-04）。</p>
     *
     * <p><b>本接口的产出即模块五的输入契约</b>：
     * {@code status = 4}、{@code finish_time}、{@code total_cost} 三个字段
     * 与完工记录共同构成下游「工单验收管理」的完整输入，无需任何适配。</p>
     *
     * @param orderId 工单 ID
     * @param dto     维修结果说明等
     * @return 变更结果（含完工时间、累计费用、新记录 ID）
     */
    OrderFinishVO finishRepair(Long orderId, OrderFinishDTO dto);

    /**
     * R-05 维修耗时统计。
     *
     * <p>按 BR-7-02 口径输出派单 / 接单 / 维修 / 验收 / 全流程五项耗时（分钟），
     * 未流转到的节点其耗时为 {@code null}。</p>
     *
     * <p><b>数据范围</b>：管理员可查任意工单；维修人员仅可查本人受派工单，
     * 否则返回 403。</p>
     *
     * @param orderId 工单 ID
     * @return 耗时明细
     */
    OrderDurationVO getOrderDuration(Long orderId);
}
