package com.community.repair.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 报修统计分析模块通用查询条件（模块七，S-01 ~ S-05 共用）。
 *
 * <p>对应接口文档 4.8 节开头的「通用筛选参数（FR-7-08）」说明：
 * {@code startTime}、{@code endTime}、{@code categoryId}、{@code workerId}
 * 适用于 S-02 ~ S-08；本模块实现的 S-01 ~ S-05 按各自接口定义，
 * 从本 DTO 中<b>按需取用</b>不同的子集：</p>
 *
 * <table border="1">
 *   <caption>各接口实际使用的参数</caption>
 *   <tr><th>接口</th><th>使用参数</th></tr>
 *   <tr><td>S-01 统计概览看板</td><td>startTime、endTime</td></tr>
 *   <tr><td>S-02 报修量趋势</td><td>granularity、startTime、endTime、categoryId、workerId、urgency</td></tr>
 *   <tr><td>S-03 类目分布</td><td>startTime、endTime、level、topN（另兼容 categoryId、workerId）</td></tr>
 *   <tr><td>S-04 工单状态分布</td><td>startTime、endTime、categoryId、workerId</td></tr>
 *   <tr><td>S-05 维修人员工作量</td><td>startTime、endTime、workerId、categoryId</td></tr>
 * </table>
 *
 * <p><b>为什么五个接口共用一个 DTO，而不是各写一个？</b>
 * 五者的筛选参数是「同一组参数的不同子集」，共用一个 DTO 可以保证
 * 时间范围、类目、维修人员三类条件在五个接口上的<b>解析方式与校验规则完全一致</b>
 * （同一份 {@code parseQueryTime} / {@code checkRange} 逻辑），
 * 避免「趋势接口按日期解析、工作量接口按日期时间解析」这类口径漂移。
 * 各接口只取自己声明的那几个字段，未声明的字段由 Controller 忽略、Service 不读取，
 * 因此不会出现「多传了参数就生效」的越权问题。</p>
 *
 * <p><b>时间入参格式</b>：同时兼容 {@code yyyy-MM-dd HH:mm:ss} 与 {@code yyyy-MM-dd} 两种写法，
 * 只传日期时「起」自动补 {@code 00:00:00}、「止」自动补 {@code 23:59:59}
 * （与模块六 E-02 的处理方式完全一致，前端 el-date-picker 的 daterange 无需拼时分秒）。</p>
 */
@Data
public class StatisticsQueryDTO implements Serializable {

    /**
     * 统计时间起（选填）。
     * <p>S-01 缺省为「当年 1 月 1 日 00:00:00」（接口文档 4.8.1）；
     * S-02 缺省为「近 30 天」（接口文档 4.8.2）；S-03 ~ S-05 缺省为「不限起始」。</p>
     */
    private String startTime;

    /**
     * 统计时间止（选填）。统一缺省为「当前时间」。
     * <p>依据 BR-7-01，所有统计一律按工单的<b>提交时间</b>（{@code repair_order.create_time}）过滤。</p>
     */
    private String endTime;

    /**
     * 报修类目 ID（选填）。
     * <p>统计口径为「工单的 {@code category_id} = 该值」，即按类目维度下钻
     * （FR-7-08「多条件筛选」）。S-01 不提供该参数（接口文档 4.8.1 仅定义时间范围）。</p>
     */
    private Long categoryId;

    /**
     * 维修人员 ID（选填）。
     * <p>统计口径为「工单的 {@code worker_id} = 该值」，即按人员维度下钻。
     * 注意 S-05 传该值时是「只看这一位维修人员」，
     * 不传时返回<b>全部维修人员的排名</b>（接口文档 4.8.5：默认按完成量降序）。</p>
     */
    private Long workerId;

    /**
     * 紧急程度筛选（选填）：0 普通 / 1 紧急（仅 S-02 使用，接口文档 4.8.2）。
     * <p>取值域与模块二 2 级口径一致（{@code OrderConstants.URGENCY_*}），越界返回 400。</p>
     */
    private Integer urgency;

    /**
     * 统计粒度（选填，仅 S-02 使用）：{@code day} 日 / {@code week} 周 / {@code month} 月，默认 {@code day}。
     * <p>取值域见 {@link com.community.repair.constant.StatisticsConstants#GRANULARITIES}，非法值返回 400。</p>
     */
    private String granularity;

    /**
     * 类目聚合层级（选填，仅 S-03 使用）：1 一级类目（默认）/ 2 二级类目。
     * <p>取值域校验统一在 Service 层完成（{@code normalizeLevel}），
     * 越界返回 400 且提示语逐字对齐接口文档 4.8.3。
     * 此处<b>不叠加</b> {@code @Min/@Max}：本 DTO 是 GET 查询串绑定的 POJO，
     * 字段级约束需要参数标注 {@code @Valid} 才会生效，而本模块刻意把
     * 全部取值域校验收敛到 Service 一处，避免出现两套错误文案。</p>
     */
    private Integer level;

    /**
     * 仅返回前 N 项（选填，仅 S-03 使用），默认 10，上限 50。
     * <p>接口文档 4.8.3 定义为「默认 10」；本模块额外约定上限 50，
     * 防止一次拉取过多类目拖慢响应（超上限不报错，按上限截断）。
     * 取值域校验同样由 Service 层的 {@code normalizeTopN} 完成。</p>
     */
    private Integer topN;
}
