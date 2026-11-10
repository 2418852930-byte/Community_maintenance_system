package com.community.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.repair.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 后即可直接使用：
 * selectById / selectOne / selectPage / insert / updateById / update 等常用方法，
 * 本模块的全部查询条件（关键字模糊、角色、状态等）使用 LambdaQueryWrapper 构造，
 * 因此无需额外编写 XML。</p>
 *
 * <p>逻辑删除由 {@code @TableLogic} 自动生效：所有查询都会自动追加 {@code deleted = 0}。</p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
