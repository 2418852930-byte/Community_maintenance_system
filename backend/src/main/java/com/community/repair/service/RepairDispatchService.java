package com.community.repair.service;

import com.community.repair.common.PageResult;
import com.community.repair.dto.BatchDispatchDTO;
import com.community.repair.dto.GrabPoolQueryDTO;
import com.community.repair.dto.GrabPublishDTO;
import com.community.repair.dto.OrderAcceptDTO;
import com.community.repair.dto.OrderAuditDTO;
import com.community.repair.dto.OrderDispatchDTO;
import com.community.repair.dto.OrderReassignDTO;
import com.community.repair.dto.OrderRejectDTO;
import com.community.repair.dto.PendingDispatchQueryDTO;
import com.community.repair.dto.WorkerWorkloadQueryDTO;
import com.community.repair.vo.BatchDispatchVO;
import com.community.repair.vo.DispatchRecordVO;
import com.community.repair.vo.GrabPoolItemVO;
import com.community.repair.vo.GrabPublishVO;
import com.community.repair.vo.OrderAcceptVO;
import com.community.repair.vo.OrderAuditVO;
import com.community.repair.vo.OrderDispatchVO;
import com.community.repair.vo.OrderGrabVO;
import com.community.repair.vo.OrderListVO;
import com.community.repair.vo.OrderReassignVO;
import com.community.repair.vo.OrderRejectVO;
import com.community.repair.vo.WorkerWorkloadVO;

import java.util.List;

/**
 * 工单派单调度模块 业务接口（模块三）。
 *
 * <p>对应需求 FR-3-01 ~ FR-3-12，接口 D-01 ~ D-12（共 12 个）。</p>
 *
 * <p>方法命名与接口编号的对应关系：</p>
 * <pre>
 *   D-01 listWorkerWorkload      维修人员负载视图（管理员）
 *   D-02 dispatchOrder           派单给维修人员（管理员）—— 0 → 1
 *   D-03 batchDispatch           批量派单（管理员）—— 0 → 1（多条）
 *   D-04 reassignOrder           改派（管理员）—— 1 → 1（仅换人）
 *   D-05 publishGrabPool         发布抢单池（管理员）—— 0 → 0（状态不变）
 *   D-06 pageGrabPool            抢单池列表（维修人员）
 *   D-07 grabOrder               抢单（维修人员）—— 0 → 1
 *   D-08 acceptOrder             接单（维修人员）—— 1 → 2
 *   D-09 rejectOrder             拒单（维修人员）—— 1 → 0（回退）
 *   D-10 listDispatchRecords     派单记录查询（管理员 / 维修人员本人）
 *   D-11 pagePendingDispatch     待派单池快捷查询（管理员）
 *   D-12 auditOrder              工单审核（管理员）—— 8 → 0 或 8 → 9
 * </pre>
 *
 * <p><b>链路定位</b>：上游承接模块二「报修工单提交」产出的<b>状态 8 待审核</b>工单，
 * 由 {@link #auditOrder} 审核通过后置为状态 0，再经派单链路
 * （{@link #dispatchOrder} / {@link #batchDispatch} / {@link #grabOrder}）
 * 流转为<b>状态 1 已派单</b>，流入模块四「维修进度管理」。</p>
 *
 * <p><b>状态流转严格遵循全局 10 态字典，禁止跳步</b>：每个写方法都在
 * {@code UPDATE ... WHERE order_id = ? AND status = ?} 的条件中固化前置状态，
 * 受影响行数不为 1 即返回 409，从数据层杜绝跳跃式流转（BR-4-02）。</p>
 *
 * <p><b>审核链路与派单链路的分界</b>：{@link #auditOrder}（8 → 0 / 8 → 9）不写入
 * {@code repair_dispatch_log}；派单链路动作逐条写入该表（BR-3-08）。</p>
 */
public interface RepairDispatchService {

    /** D-01 维修人员负载视图 */
    List<WorkerWorkloadVO> listWorkerWorkload(WorkerWorkloadQueryDTO query);

    /** D-02 派单给维修人员 */
    OrderDispatchVO dispatchOrder(Long orderId, OrderDispatchDTO dto);

    /** D-03 批量派单 */
    BatchDispatchVO batchDispatch(BatchDispatchDTO dto);

    /** D-04 改派 */
    OrderReassignVO reassignOrder(Long orderId, OrderReassignDTO dto);

    /** D-05 发布抢单池 */
    GrabPublishVO publishGrabPool(Long orderId, GrabPublishDTO dto);

    /** D-06 抢单池列表 */
    PageResult<GrabPoolItemVO> pageGrabPool(GrabPoolQueryDTO query);

    /** D-07 抢单 */
    OrderGrabVO grabOrder(Long orderId);

    /** D-08 接单 */
    OrderAcceptVO acceptOrder(Long orderId, OrderAcceptDTO dto);

    /** D-09 拒单 */
    OrderRejectVO rejectOrder(Long orderId, OrderRejectDTO dto);

    /** D-10 派单记录查询 */
    List<DispatchRecordVO> listDispatchRecords(Long orderId);

    /** D-11 待派单池快捷查询（等价于 O-02 且 status = 0） */
    PageResult<OrderListVO> pagePendingDispatch(PendingDispatchQueryDTO query);

    /** D-12 工单审核（通过 / 驳回） */
    OrderAuditVO auditOrder(Long orderId, OrderAuditDTO dto);
}
