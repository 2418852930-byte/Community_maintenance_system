package com.community.repair;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 社区便民报修服务管理系统 —— 启动类
 *
 * <p>当前已实现模块：</p>
 * <ul>
 *   <li>模块零 登录注册与用户管理模块（需求 FR-8-01 ~ FR-8-12，接口 U-01 ~ U-14、U-15）；</li>
 *   <li>模块一 报修类目管理模块（需求 FR-1-01 ~ FR-1-06，接口 C-01 ~ C-07）；</li>
 *   <li>模块二 报修工单提交模块（需求 FR-2-01 ~ FR-2-09，接口 O-01 ~ O-06）。</li>
 * </ul>
 *
 * @author 毕业设计
 */
@SpringBootApplication
// 扫描 Mapper 接口所在包，等价于在每个 Mapper 上加 @Mapper 注解
@MapperScan("com.community.repair.mapper")
public class CommunityRepairApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityRepairApplication.class, args);
        System.out.println("""
                ============================================================
                  社区便民报修服务管理系统 启动成功
                  后端地址：http://localhost:8080
                  已实现模块：登录注册与用户管理、报修类目管理、报修工单提交
                  免鉴权接口：/api/auth/register  /api/auth/login  /api/auth/forgot-password
                  图片访问前缀：/uploads/**（由 U-15 上传接口写入）
                  默认管理员：admin / 123456（见建表 SQL 初始化数据）
                  默认居民：resident01 / 123456（提交报修工单用）
                ============================================================
                """);
    }
}
