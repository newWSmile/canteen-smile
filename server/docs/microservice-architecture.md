# 微服务架构设计

## 当前拓扑

```text
Browser / App
     |
     v
smile-gateway (WebFlux + Sa-Token)
     |
     +---- /api/auth/v1/** 静态路由 --> smile-auth-service
     |
     +---- /api/iam/v1/** 静态路由 ---> smile-iam-service
     |
     +---- 未来经领域评审新增 --------> smile-<domain>-service

Redis: Sa-Token 分布式会话；业务缓存按服务命名空间隔离
PostgreSQL: 每个服务拥有自己的 schema/数据库边界，禁止跨服务查表
```

当前网关通过 `AUTH_SERVICE_URI` 和 `IAM_SERVICE_URI` 分别路由 Auth 与 IAM 的 v1 外部接口。`/internal/**` 不经 Gateway 暴露。服务发现暂不接入 Nacos，两个地址均按环境静态配置；登录白名单只在对应真实 Controller 落地时按最小路径添加。

## 职责

- `smile-gateway`：TLS 终止（部署层）、路由、跨域策略、限流入口、TraceId 传播和粗粒度登录认证。不承载业务逻辑，不代替下游最终鉴权。
- `smile-auth-service`：拥有认证凭证、验证码、激活重置、Token 和设备会话，不拥有租户、机构、角色或权限事实。
- `smile-iam-service`：拥有租户、机构、用户资料、角色、菜单、权限和数据范围，独占 IAM 数据库。
- `smile-common`：只包含必须跨服务稳定一致的基础契约，严禁演变成“大杂烩”。
- 两个内部 starter：统一横切配置；不得反向依赖任何业务服务。

## 新服务准入清单

1. 领域能力和负责人明确，能说明为何不能放入现有服务。
2. 数据所有权、主键策略和迁移脚本归属明确。
3. REST/事件契约、调用方、版本兼容策略明确。
4. 接口权限、组织权限与 SQL 数据范围明确。
5. 超时、幂等、有限重试、熔断、降级和补偿方案明确。
6. 容量、分页、慢查询、线程池、连接池、P95/P99 与告警目标明确。

## 已确认决策与待确认项

- Auth 与 IAM 当前使用 REST Client + 静态服务地址，后续接入 Nacos 时保持路径和 DTO 契约不变。
- 主键统一 PostgreSQL sequence + bigint；Auth 与 IAM 使用独立数据库。
- 关键跨服务通知使用本地 Outbox + 可重试投递，后续可平滑接入消息队列。
- 内部调用采用网络隔离 + HMAC 请求签名。
- `TODO(待确认)`：Nacos 的接入阶段和生产配置治理方案。
- 数据库脚本当前集中在根目录 `sql`，按 Auth/IAM 和 DDL/DML 人工迭代；暂不引入 Flyway。未来接入迁移工具必须先制定人工基线接管方案。
- `TODO(待确认)`：密码哈希参数、短信/验证码供应商和 XXL-JOB 真实环境参数。

完整领域基线见 `iam-enterprise-design.md`，可执行接口、数据库和实施顺序见根目录 `doc/iam-executable-implementation-plan.md`。
