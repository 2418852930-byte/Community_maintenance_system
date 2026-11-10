package com.community.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.repair.entity.RepairDispatchLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 派单记录表 Mapper（{@code repair_dispatch_log}）。
 *
 * <p>对应需求 FR-3-07、BR-3-08，接口 D-10 及派单链路上各动作的审计写入。</p>
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 后即可直接使用
 * selectById / selectList / selectPage / selectCount / insert 等常用方法。
 * 本模块的查询条件（按工单、按操作类型）全部使用 {@code LambdaQueryWrapper} 构造，
 * 与模块一 / 模块二保持一致，<b>不引入 XML 映射文件</b>。</p>
 *
 * <p><b>只追加约定</b>：本表业务上仅「新增（insert）」与「查询（select）」，
 * 本 Mapper 不提供任何修改 / 删除的封装方法，从代码层面保证审计轨迹不可篡改。</p>
 */
@Mapper
public interface RepairDispatchLogMapper extends BaseMapper<RepairDispatchLog> {
}
