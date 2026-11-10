package com.community.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.repair.entity.RepairRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 维修记录表 Mapper（{@code repair_record}）。
 *
 * <p>本 Mapper 由<b>模块三</b>随 D-08 接单、D-09 拒单两个接口一并建立
 * （这两个接口必须向本表写入「接单 / 转派退回」记录），
 * 后续<b>模块四「维修进度管理」可直接复用</b>，无需重复定义实体与 Mapper。</p>
 *
 * <p>本模块只使用 {@code insert} 一种写操作，所有记录类型常量集中在
 * {@code DispatchConstants.RECORD_TYPE_*}。</p>
 */
@Mapper
public interface RepairRecordMapper extends BaseMapper<RepairRecord> {
}
