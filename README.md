# Canteen Smile

前后端分离的基础工程，认证能力基于 Sa-Token。当前仓库仅建立可扩展的工程骨架和横切基础设施；在用户、组织、角色、权限及数据库模型确认之前，不提供虚构的登录接口或业务字段。

## 目录

- `web`：Vue 3 + Vite + TypeScript 前端。
- `server`：Spring Boot 3 + Spring Cloud Gateway + Java 17 + Sa-Token + MyBatis 的 Maven 多模块微服务后端，包含独立 Gateway、Auth 和 IAM 服务。
- `doc`：已经确认的跨前后端可执行落地方案。
- `sql`：IAM/Auth 两个 PostgreSQL 数据库的 DDL、DML 和人工执行记录；当前不使用 Flyway。
- `AGENTS.md`：开发者和 AI 协作规则。
- `项目宪法.md`：不可绕过的工程约束。

## 本地启动

前端：

```bash
cd web
pnpm install
pnpm dev
```

后端所需环境变量示例见 `server/.env.example`。准备 PostgreSQL 与 Redis 后：

```bash
cd server
mvn install
mvn -pl smile-gateway spring-boot:run
# 另一个终端
mvn -pl smile-auth-service spring-boot:run
# 第三个终端
mvn -pl smile-iam-service spring-boot:run
```

## Sa-Token 采用方式

- 使用 `sa-token-spring-boot3-starter` 和 Jackson Redis DAO。
- 网关与下游服务对 `/api/**` 执行分层鉴权，公开路径必须经安全评审后显式加入白名单。
- 前端统一通过请求头 `satoken` 携带令牌；令牌值由真实登录接口返回后再接入，禁止临时伪造。
- 权限、角色和数据范围依赖真实业务表，当前不注册虚假的 `StpInterface` 实现，也不虚构网关路由。
