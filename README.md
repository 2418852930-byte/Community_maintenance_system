# 社区便民报修服务管理系统

面向社区居民与物业维修团队的报修全流程管理系统，覆盖「用户报修 → 派单调度 → 维修进度 → 工单验收 → 服务评价 → 统计分析」的完整闭环。

## 技术栈

| 层次 | 技术选型 |
| --- | --- |
| 后端 | Spring Boot 3.1.5 · MyBatis-Plus 3.5.5 · JWT · Java 17 |
| 数据库 | MySQL 8.x |
| 前端 | Vue 3 · Element Plus · Pinia · Vue Router · ECharts · Axios |
| 构建 | Maven（后端） · Vite 5（前端） |

## 功能模块

| 模块 | 说明 | 前端页面 |
| --- | --- | --- |
| 零 · 登录注册与用户管理 | 注册、登录、找回密码、个人中心、用户管理 | Login / Register / ForgotPassword / Profile / UserManage |
| 一 · 报修类目管理 | 报修分类的增删改查与启停 | CategoryManage |
| 二 · 报修工单提交 | 居民提交报修工单、上传故障图片 | OrderSubmit / RepairRecordForm / MyOrders |
| 三 · 工单派单调度 | 管理员派单、维修工抢单 | OrderDispatch / GrabHall / MyTasks |
| 四 · 维修进度管理 | 维修工更新处理进度与维修记录 | OrderDetail |
| 五 · 工单验收管理 | 管理员审核、居民验收工单 | OrderAudit / OrderVerify |
| 六 · 服务评价管理 | 居民对已完成工单评价、查看我的评价 | EvaluationManage / MyEvaluations |
| 七 · 报修统计分析 | 工单量、类目分布、处理效率等图表看板 | StatisticsBoard / Dashboard |

## 目录结构

```
社区维修系统/
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/community/repair/
│       │   ├── controller/      # 接口层（Auth / Order / Dispatch / Progress / Verify / Evaluation / Statistics / User / File ...）
│       │   ├── service/         # 业务层 + impl 实现
│       │   ├── mapper/          # MyBatis-Plus Mapper
│       │   ├── entity/          # 数据库实体
│       │   ├── dto/ vo/ convert/# 数据传输对象与转换
│       │   ├── security/        # JWT 鉴权
│       │   ├── config/ common/ constant/ enums/ util/
│       └── resources/
│           └── application.yml  # 数据源、JWT、文件上传等配置
└── frontend/                    # Vue 3 前端
    ├── vite.config.js
    ├── package.json
    └── src/
        ├── api/                 # 接口封装（auth / category / order / dispatch / progress / verify / evaluation / statistics / user / file）
        ├── views/               # 页面
        ├── components/ layout/  # 公共组件与布局
        ├── router/ store/       # 路由与 Pinia 状态
        └── utils/ constants/ styles/
```

## 本地运行

### 1. 准备数据库

创建数据库并执行建表脚本（如表结构未随仓库提供，请按 `entity` 包中的实体自行建表）：

```sql
CREATE DATABASE community_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 2. 启动后端

修改 `backend/src/main/resources/application.yml` 中的数据库连接信息（地址、账号、密码），然后：

```bash
cd backend
mvn spring-boot:run
```

后端默认端口见 `application.yml`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

`vite.config.js` 中已配置开发代理，前端请求会转发到后端。

### 4. 打包构建

```bash
cd frontend && npm run build      # 产物在 frontend/dist
cd backend && mvn clean package   # 产物在 backend/target
```

## 说明

- 仓库不包含 `node_modules`、`target`、IDE 配置等本地文件，克隆后需自行安装依赖。
- `application.yml` 中的数据库账号密码为本地开发配置，部署到服务器前请改为环境变量或外部配置。
