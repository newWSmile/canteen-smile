# 项目协作说明

本文件适用于仓库全部目录。开始修改前必须先阅读本文件和根目录《项目宪法.md》；进入 `web` 或 `server` 后，还需阅读对应目录的 `README.md`。

## 修改前检查

1. 先使用 `rg --files` 阅读目录，再检查 `web/package.json`、`server/pom.xml` 与同类实现。
2. 确认现有统一请求、返回、异常、认证、Redis、数据库及命名规范，不得另起一套重复实现。
3. 不得猜测接口地址、请求/响应字段、数据库字段、权限码、Redis Key、字典值或第三方接口；无法从代码、数据库迁移或已确认文档中得到的信息必须标记 `TODO(待确认)` 并停止相关实现。
4. 坚持最小改动，不升级依赖、不重构无关代码、不批量格式化。

## 前端约束

- 仅使用 Vue 3 Composition API、Vite、TypeScript 和 pnpm，禁止引入 Vue 2 写法或依赖。
- 前端工作区固定使用 Node.js 20.19.0 和 pnpm 10.34.5，不得使用 pnpm 11 安装依赖或改写锁文件。
- 业务按 `src/modules/<module>` 分包；跨模块能力仅放入 `src/shared`，应用装配放入 `src/app`。
- 所有 HTTP 请求只能经 `src/shared/http/client.ts` 导出的 Axios 实例发送。
- 成功、失败、警告反馈只能经 `src/shared/feedback` 统一出口展示。
- 写操作按钮使用 `useSingleFlight` 控制提交态；Axios 层同时拦截相同的并发请求。
- API 类型只能依据真实后端契约定义，不得用 `any`、假数据或臆测字段补齐页面。
- 管理页面必须复用统一的标题区、主操作按钮、查询工具栏、列表面板、行操作和分页样式；查询按钮禁止被布局拉伸为整行，同类弹窗的表单间距与按钮顺序必须一致。

## 后端与微服务约束

- `server` 是 Maven 多模块微服务工程，不得退化为单个 Spring Boot 服务。可部署服务使用 `smile-<domain>-service` 命名，共享模块使用 `smile-<capability>-starter` 或 `smile-common`。
- 使用 Java 17、Spring Boot、Spring Cloud Gateway、Maven、PostgreSQL、Redis、MyBatis、Jackson、SLF4J + Logback 和 Sa-Token。
- 外部流量统一进入 `smile-gateway`；网关做登录认证和粗粒度路由校验，各业务服务必须再次执行接口权限、数据权限和组织权限的最终校验。
- 每个服务按业务边界独立部署、独立配置、独立拥有数据；禁止跨服务直接访问对方数据库或 Mapper，禁止把多个领域继续堆入认证服务。
- 服务间调用必须经显式 `Client` 与版本化 DTO 契约，设置连接/读取超时，并按幂等性设计重试；不得共享 Entity，不得形成循环依赖。
- 调用方向固定为 `Controller -> Service -> Mapper`；Controller 不得访问 Mapper、Redis 或第三方 Client。
- Entity 仅映射数据库表，DTO 负责入参/服务传递，VO 负责响应；Entity 不得直接出现在 Controller 的入参或返回值中。
- Java 数据对象和构造器样板代码优先使用 Lombok 的 `@Getter`、`@Setter`、`@RequiredArgsConstructor`；Entity 禁止使用 `@Data`。静态接口权限优先使用 Sa-Token 注解并引用集中权限常量，禁止在 Controller 散落权限字符串或直接调用 `StpUtil.checkPermission`。
- 类、方法、字段、变量和枚举必须有清晰注释；新增 REST 入参必须使用 Spring Validation。
- 事务放在公开 Service 方法；数据权限必须下推 SQL，列表必须分页且限制页大小。
- 所有 PostgreSQL DDL、DML 必须集中放在根目录 `sql/<service>/{ddl,dml}`，按 `sql/README.md` 编号、登记和人工执行；当前禁止在服务资源目录另建迁移脚本或擅自引入 Flyway。已在共享环境执行的 SQL 不得修改，只能新增更高编号修正脚本。
- 所有 PostgreSQL 表和每一个字段都必须使用 `COMMENT ON TABLE`、`COMMENT ON COLUMN` 添加明确的中文用途说明；建表或新增字段时必须在同一迭代脚本内补齐，字段中文注释覆盖率未达到 100% 不得交付。已执行脚本缺少注释时，只能通过更高编号修正脚本补齐。
- 第三方能力放入独立 Client；异步任务只能使用项目统一线程池。
- 敏感值只从环境变量、配置中心或密钥服务读取，不得提交真实凭据。
- 新增服务前必须确认领域边界、数据所有权、调用方、接口契约、容量与故障策略；不得只为“看起来像微服务”创建无业务边界的空服务。

## 完成标准

- 修改后至少执行与改动相关的编译、类型检查或测试。
- 后端验证按变更影响范围执行。仅调整局部字段、方法参数或方法内部实现，且未改变模块边界、公共契约或整体结构时，不要求运行整个 `server` 的全量测试；应至少编译受影响模块，并按需运行直接相关的测试。出现明显结构变动、删除整个类、移除整个模块或其它可能产生跨模块影响的变更时，必须扩大验证范围并运行后端全量相关测试。
- 不得在没有运行验证时宣称完成；受环境阻塞时必须说明未验证项及原因。
- 不得使用 H2 替代 PostgreSQL 来证明 PostgreSQL 专属行为正确。
