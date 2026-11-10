package com.community.repair.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器。
 *
 * <p>背景（对应《02-建表SQL（方案A）》的兼容改造说明）：</p>
 * <ul>
 *   <li>{@code create_time} 是 TIMESTAMP，带 DEFAULT CURRENT_TIMESTAMP，本可由数据库维护；
 *       但为了统一，这里同样在插入时显式赋值。</li>
 *   <li>{@code update_time} 是 DATETIME，默认值固定为 '1970-01-01 00:00:01'，
 *       <b>必须由业务代码显式维护</b>，否则新增/修改后时间不会变。</li>
 * </ul>
 *
 * <p>生效前提：实体字段上标注 {@code @TableField(fill = FieldFill.INSERT / INSERT_UPDATE)}。</p>
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /** 新增时填充 createTime 与 updateTime */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 使用 strictInsertFill 可避免覆盖调用方已显式设置的值
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    /** 修改时填充 updateTime */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
