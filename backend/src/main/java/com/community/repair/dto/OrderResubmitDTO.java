package com.community.repair.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 驳回工单修改后重新提交请求参数（对应接口 O-06：PUT /api/orders/{orderId}/resubmit）。
 *
 * <p>依据：接口文档 4.3.6（V2.0 新增）；需求 FR-2-09、BR-2-09、BR-3-10。</p>
 *
 * <p><b>为什么继承 {@link OrderUpdateDTO} 而不是重新声明 9 个字段？</b></p>
 * <ol>
 *   <li>接口文档明确规定「O-06 字段校验规则与 O-01 一致」，O-04 亦为「字段校验规则同 O-01」，
 *       三者的字段集合与约束完全相同，重复声明会造成校验注解的三处维护；</li>
 *   <li>继承后 O-04 与 O-06 共用同一份约束定义，<b>唯一的差异集中在 Service 层的状态域判断</b>
 *       （O-04 只允许状态 8 / 0，O-06 只允许状态 9），职责边界更清晰；</li>
 *   <li>O-06 独有的一条规则——「至少须提交一个可修改字段」（NULL 全为空则 400）——
 *       属于跨字段校验，即便单独声明也无法用字段级注解表达，本就应放在 Service 层。</li>
 * </ol>
 * <p>该规则在 {@code RepairOrderServiceImpl.resubmitOrder} 中实现，提示语为
 * 「请至少修改一项内容后再提交」。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderResubmitDTO extends OrderUpdateDTO implements Serializable {
}
