package com.community.repair.controller;

import com.community.repair.common.Result;
import com.community.repair.constant.DispatchConstants;
import com.community.repair.dto.WorkerWorkloadQueryDTO;
import com.community.repair.enums.RoleEnum;
import com.community.repair.security.RequireRole;
import com.community.repair.service.RepairDispatchService;
import com.community.repair.vo.WorkerWorkloadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 维修人员类接口 Controller（D-01）。
 *
 * <p>对应需求 FR-3-06，接口文档 4.4.1 节。</p>
 *
 * <p><b>为什么与 {@code RepairDispatchController} 分开？</b>
 * 因为接口设计文档为本接口规定的 URL 前缀是 {@code /api/workers}，
 * 与派单调度其余接口的 {@code /api/orders} 不同。按 URL 前缀拆分 Controller
 * 是本项目一贯的组织方式（模块零的 {@code AuthController} 与 {@code UserController}
 * 也是按前缀拆分），保持一致。</p>
 *
 * <p><b>权限</b>：类上标注 {@code @RequireRole(RoleEnum.ADMIN)}，
 * 由 {@link com.community.repair.security.JwtInterceptor} 统一做服务端角色校验：
 * 非管理员访问返回 403，且不返回任何数据。本接口<b>不新增任何鉴权组件</b>。</p>
 */
@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
@RequireRole(RoleEnum.ADMIN)
public class WorkerController {

    private final RepairDispatchService repairDispatchService;

    /**
     * D-01 维修人员负载视图。
     *
     * <p>GET /api/workers/workload　仅系统管理员</p>
     *
     * <p>返回维修人员列表及其在建工单数量，辅助管理员均衡派单。
     * 在途量按状态 1~4 统计，处理中按 2~3，完成量按 5~6；
     * 待审核(8) / 已驳回(9) / 已撤销(7) 未进入派单链路，不计入任何工作量指标。</p>
     *
     * @param query workerType 按工种筛选；onlyEnabled 默认 true 仅返回账号正常的人员
     */
    @GetMapping("/workload")
    public Result<List<WorkerWorkloadVO>> listWorkerWorkload(WorkerWorkloadQueryDTO query) {
        return Result.success(DispatchConstants.MSG_QUERY_SUCCESS,
                repairDispatchService.listWorkerWorkload(query));
    }
}
