package com.community.repair.mapper;

import com.community.repair.vo.WorkerWorkloadStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单统计类 Mapper（<b>只读</b>，专供模块三 D-01 维修人员负载视图使用）。
 *
 * <p>为什么单独建一个 Mapper，而不是往模块二的 {@code RepairOrderMapper} 里追加方法？
 * 两个理由：</p>
 * <ol>
 *   <li><b>不修改前序模块</b>：模块二的 {@code RepairOrderMapper} 属于「报修工单提交模块」
 *       的交付边界，本模块遵循「能新增就不修改」的原则，一个字节都不动它；</li>
 *   <li><b>读写分离语义清晰</b>：本 Mapper 只承载<b>聚合统计型</b>查询（GROUP BY 结果），
 *       与 {@code RepairOrderMapper} 的「实体读写」职责分开，接口职责单一，便于论文论述。</li>
 * </ol>
 *
 * <p>本 Mapper 不继承 {@code BaseMapper}，因为负载统计不需要任何实体 CRUD 能力。</p>
 */
@Mapper
public interface RepairOrderStatMapper {

    /**
     * 按维修人员聚合工单量（D-01 维修人员负载视图的核心查询）。
     *
     * <p><b>实现要点一：统计口径由常量传入，不写死在 SQL 里。</b>
     * 三个状态分组（在途 / 处理中 / 累计完成）由 Service 层从
     * {@code DispatchConstants.WORKLOAD_*_STATUSES} 常量数组传入，
     * 从而保证「状态分组口径」全项目只有一处定义，避免 SQL 与 Java 常量各写一份而漂移。</p>
     *
     * <p><b>实现要点二：状态值走 {@code #{} 预编译参数，不做字符串拼接</b>，
     * 从机制上杜绝 SQL 注入（即使未来常量数组改由外部配置驱动也依然安全）。</p>
     *
     * <p><b>实现要点三：只扫 {@code repair_order} 一张表</b>，
     * 用 {@code COUNT(CASE WHEN ... END)} 一次算出三个指标，避免三次往返查询；
     * 走 {@code idx_worker(worker_id)} 索引分组。</p>
     *
     * @param pendingStatuses    在途状态集合（1,2,3,4）
     * @param processingStatuses 处理中状态集合（2,3）
     * @param finishedStatuses   累计完成状态集合（5,6）
     * @return 每位有在册工单的维修人员一条聚合记录；无工单的维修人员不在此结果中
     */
    @Select("<script>"
            + "SELECT o.worker_id AS worker_id, "
            + "COUNT(CASE WHEN o.status IN "
            + "<foreach collection='pendingStatuses' item='s' open='(' separator=',' close=')'>#{s}</foreach> "
            + "THEN 1 END) AS pending_count, "
            + "COUNT(CASE WHEN o.status IN "
            + "<foreach collection='processingStatuses' item='s' open='(' separator=',' close=')'>#{s}</foreach> "
            + "THEN 1 END) AS processing_count, "
            + "COUNT(CASE WHEN o.status IN "
            + "<foreach collection='finishedStatuses' item='s' open='(' separator=',' close=')'>#{s}</foreach> "
            + "THEN 1 END) AS finished_count "
            + "FROM repair_order o "
            + "WHERE o.deleted = 0 AND o.worker_id IS NOT NULL "
            + "GROUP BY o.worker_id"
            + "</script>")
    List<WorkerWorkloadStatVO> selectWorkerWorkloadStat(
            @Param("pendingStatuses") int[] pendingStatuses,
            @Param("processingStatuses") int[] processingStatuses,
            @Param("finishedStatuses") int[] finishedStatuses);
}
