package com.community.repair.service;

import com.community.repair.common.PageResult;
import com.community.repair.dto.OrderCancelDTO;
import com.community.repair.dto.OrderCreateDTO;
import com.community.repair.dto.OrderQueryDTO;
import com.community.repair.dto.OrderResubmitDTO;
import com.community.repair.dto.OrderUpdateDTO;
import com.community.repair.vo.OrderCancelVO;
import com.community.repair.vo.OrderCreateVO;
import com.community.repair.vo.OrderDetailVO;
import com.community.repair.vo.OrderListVO;
import com.community.repair.vo.OrderResubmitVO;
import com.community.repair.vo.OrderUpdateVO;

/**
 * 报修工单提交模块 业务接口。
 *
 * <p>对应需求 FR-2-01 ~ FR-2-09，接口 O-01 ~ O-06（共 6 个）。</p>
 *
 * <p>方法命名与接口编号的对应关系：</p>
 * <pre>
 *   O-01 createOrder     提交报修工单（仅居民）—— 初始状态固定 8 待审核
 *   O-02 pageOrders      工单分页列表（全部登录用户，按角色自动限定数据范围）
 *   O-03 getOrderDetail  工单详情（全部登录用户，受数据归属约束）
 *   O-04 updateOrder     修改工单（仅居民，状态域 8 / 0）
 *   O-05 cancelOrder     撤销工单（居民本人、管理员，状态域 8 / 0 / 9）
 *   O-06 resubmitOrder   驳回工单修改后重新提交（仅居民，状态域仅 9）
 * </pre>
 *
 * <p><b>链路定位</b>：上游承接模块一「报修类目管理」的类目字典（提交前校验类目启用状态），
 * 下游产出「待审核」工单流入模块三「工单派单调度」，由 D-12 审核（8 → 0）后方进入派单链路。</p>
 */
public interface RepairOrderService {

    /** O-01 提交报修工单 */
    OrderCreateVO createOrder(OrderCreateDTO dto);

    /** O-02 工单分页列表 */
    PageResult<OrderListVO> pageOrders(OrderQueryDTO query);

    /** O-03 工单详情 */
    OrderDetailVO getOrderDetail(Long orderId);

    /** O-04 修改工单 */
    OrderUpdateVO updateOrder(Long orderId, OrderUpdateDTO dto);

    /** O-05 撤销工单 */
    OrderCancelVO cancelOrder(Long orderId, OrderCancelDTO dto);

    /** O-06 驳回工单修改后重新提交 */
    OrderResubmitVO resubmitOrder(Long orderId, OrderResubmitDTO dto);
}
