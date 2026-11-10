package com.community.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.repair.entity.RepairVerifyLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 验收记录表 Mapper（模块五）。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 后即可直接使用
 * {@code insert / selectList / selectOne / selectCount} 等方法，
 * 查询条件全部用 {@code LambdaQueryWrapper} 构造，
 * 与模块二 / 三 / 四的做法保持一致，<b>不引入 XML 映射文件</b>。</p>
 *
 * <p>逻辑删除由 {@code @TableLogic} 自动生效：所有查询都会自动追加 {@code deleted = 0}。</p>
 *
 * <p><b>本 Mapper 只暴露「新增 + 查询」能力</b>：本模块不提供任何修改 / 删除验收记录的接口，
 * 与 BR-5-05「每次验收均独立留痕」的设计意图一致。</p>
 */
@Mapper
public interface RepairVerifyLogMapper extends BaseMapper<RepairVerifyLog> {

    /**
     * 查询某工单<b>当前最大</b>的验收轮次，用于生成下一轮 {@code verify_round}。
     *
     * <p>实现说明：{@code repair_verify_log} 上已有唯一索引
     * {@code uk_order_round (order_id, verify_round)}，本查询可直接走该索引的
     * 「有序扫描 + 取首行」，效率为 O(1)；同时由该唯一索引为并发验收兜底
     * （Service 层捕获冲突后返回 409）。</p>
     *
     * <p>返回 {@code null} 表示该工单尚无任何验收记录，下一轮次从 1 开始。</p>
     *
     * <p><b>注意</b>：这里用 {@code MAX(verify_round)} 而不是 {@code COUNT(*)}，
     * 因为轮次是<b>业务序号</b>——即使未来出现逻辑删除（deleted = 1）的记录，
     * 轮次也应当继续递增而不复用，避免 V-04 展示出现「两轮都叫第 2 轮」的歧义。</p>
     *
     * @param orderId 工单ID
     * @return 当前最大验收轮次；无记录时返回 null
     */
    @Select("SELECT MAX(verify_round) FROM repair_verify_log "
            + "WHERE order_id = #{orderId} AND deleted = 0")
    Integer selectMaxVerifyRound(@Param("orderId") Long orderId);
}
