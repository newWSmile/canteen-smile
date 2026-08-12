# IAM 企业级体系可执行落地方案

> 文档状态：实施中  
> 业务依据：`server/docs/iam-enterprise-design.md`  
> 适用工程：`web` 三应用工作区、`server` Maven 多模块微服务工程  
> 重要说明：本文中的 REST 路径、DTO/VO、表名、字段名、权限码和 Redis Key 都是本次设计形成的**目标契约**，当前代码尚未实现。进入编码后应以版本化迁移和契约测试固化，不得把本文误认为现有接口。

实施记录（2026-08-06）：第一批已经落地统一 `PageResult`、业务异常 HTTP 状态、独立 `smile-iam-service` 工程边界以及 Gateway 的 Auth/IAM v1 静态路由；第二批已建立根目录 `sql`，按 IAM/Auth、DDL/DML 进行人工版本迭代，当前不引入 Flyway；用户已确认手工执行 `IAM_DDL_0001`、`IAM_DDL_0002`，这两个脚本已冻结；`IAM_DDL_0003` 已形成草案，覆盖账号、角色和机构所有者。登录 Controller、权限码和业务页面仍属于后续实施项，未实现的部分不得当作现有能力。

## 1. 目标、范围与当前差距

本方案把已经确认的 IAM 业务规则落实为可分阶段开发的前端页面、后端服务、接口契约、数据库模型、安全机制、测试和上线步骤。范围包括：

- 平台身份与租户身份隔离；
- 租户、机构类型、机构树和行政区域关联；
- 用户、机构所有者、角色、菜单、按钮、接口权限和数据权限；
- 用户名密码、手机号验证码、账号选择、激活、绑定、找回和管理员重置；
- 多设备会话、记住我、密码策略、授权版本和强制下线；
- HMAC 服务间认证、本地事件表、可重试投递和安全审计；
- 三个独立前端入口及其共享边界。

当前工程状态：

| 项目 | 当前状态 | 本方案要求 |
| --- | --- | --- |
| `web/apps/platform-admin` | 独立 Vue 3 骨架 | 实现平台管理端页面与独立会话 |
| `web/apps/tenant-admin` | 独立 Vue 3 骨架 | 实现租户 IAM 管理端 |
| `web/apps/tenant-portal` | 独立 Vue 3 骨架 | 实现租户用户登录、个人安全和业务入口 |
| `smile-gateway` | `/api/**` 静态转发 Auth | 按 `/api/auth/**`、`/api/iam/**` 收窄路由 |
| `smile-auth-service` | 仅服务骨架 | 实现认证、凭证、验证码和会话领域 |
| `smile-iam-service` | 尚不存在 | 新增独立服务、数据库和部署单元 |
| 公共能力 | 已有统一响应、异常、Redis Key 构建器 | 复用并增补分页、内部签名和授权上下文能力 |

本阶段不设计食堂、采购、库存等业务页面和业务字段。业务模块只有在确认真实数据归属字段后，才能接入数据权限。

## 2. 总体架构

```text
platform-admin ─┐
tenant-admin ───┼──> smile-gateway :8080
tenant-portal ──┘          │
                           ├── /api/auth/v1/** ──> smile-auth-service
                           └── /api/iam/v1/**  ──> smile-iam-service
                                                        │
                           Auth <── REST + HMAC ────────> IAM

PostgreSQL cluster
├── smile_auth database     # Auth 独占
└── smile_iam database      # IAM 独占

Redis Cluster DB 0
└── Sa-Token、短期挑战、限流、Nonce、版本和权限快照
```

### 2.1 服务职责

`smile-auth-service` 只拥有认证秘密和会话：密码摘要及历史、已验证手机号、短信挑战、激活/重置凭证、登录失败、平台恢复码、设备会话、Auth 审计和事件消费幂等记录。

`smile-iam-service` 只拥有身份及授权事实：平台身份、租户、机构、账号资料、所有者、角色、权限资源、数据范围、菜单配置、授权版本、IAM 审计和待投递事件。

任何服务不得直接查询另一个服务的数据库，不共享 Entity、Mapper 或事务。Gateway 只做登录和粗粒度版本检查，IAM 及未来业务服务必须完成最终接口、组织和数据权限校验。

### 2.2 建议 Maven 模块变化

```text
server/
├─ smile-common/                  # ApiResponse、公共错误和版本化内部契约的基础类型
├─ smile-web-starter/             # 全局异常、Validation、TraceId、Sa-Token Web 配置
├─ smile-infrastructure-starter/  # MyBatis、Redis、线程池
├─ smile-gateway/                 # 外部唯一入口
├─ smile-auth-service/            # 可独立部署，独占 Auth 数据
└─ smile-iam-service/             # 新增，可独立部署，独占 IAM 数据
```

暂不为了“共享授权”新增空 starter。等第二个业务服务真实接入数据权限时，再把稳定、无领域数据所有权的授权上下文解析能力提取为 starter。

### 2.3 服务内包结构

IAM 按领域模块分包，而不是把全部类堆入 `modules.iam`：

```text
com.canteen.smile
├─ modules.platform/       # 平台身份、平台管理员、恢复码协作
├─ modules.tenant/         # 租户生命周期和安全策略
├─ modules.organization/   # 机构类型、关系、机构树、所有者
├─ modules.account/        # 租户账号资料、用户名和工号保留
├─ modules.role/           # 角色和用户角色
├─ modules.permission/     # 权限资源、租户功能、菜单显示
├─ modules.datascope/      # 模块定义、角色范围和 SQL 授权上下文
├─ modules.audit/          # IAM 管理审计
├─ modules.outbox/         # 本地事件表及投递
└─ internal/               # HMAC 内部 Controller、Client 和 DTO v1
```

每个模块内部使用 `controller -> service -> mapper`，并按需设置 `dto`、`vo`、`entity`、`converter`、`client`。Auth 对应拆为 `login`、`credential`、`mobile`、`challenge`、`activation`、`reset`、`session`、`recovery`、`audit`、`internal`。

## 3. 前端三应用设计

### 3.1 共享原则

- 三个应用独立构建、部署、Token 存储和路由白名单；不得共享登录态。
- 所有请求只经各应用 `src/shared/http/client.ts` 导出的 Axios 实例发送。
- 成功、失败、警告只经 `src/shared/feedback`；写操作使用 `useSingleFlight`，Axios 再拦截同指纹并发请求。
- 后端接口形成真实契约后，类型才进入 `web/packages/contracts`；应用专属页面模型留在对应模块。
- Pinia 只保存必要的当前身份、权限码、菜单树和安全版本；手机号、重置链接、验证码、密码不得持久化。
- 路由 `meta.permission` 仅决定显示和导航，不能替代服务端校验。

### 3.2 platform-admin 页面

| 路由 | 页面 | 核心动作 | 权限 |
| --- | --- | --- | --- |
| `/login` | 平台密码登录 | 当前用户名密码直接登录；预留风险二次验证 | 匿名 |
| `/recovery` | 平台恢复流程 | 使用一次性恢复码设置新密码 | 匿名、受限流程 |
| `/dashboard` | 平台总览 | 租户状态数量、安全事件摘要 | `platform:dashboard:view` |
| `/tenants` | 租户列表 | 分页、状态过滤、创建 | `platform:tenant:view/create` |
| `/tenants/:id` | 租户详情 | 基本信息、状态、安全版本 | `platform:tenant:view` |
| `/tenants/:id/lifecycle` | 租户生命周期 | 暂停、恢复、到期、注销 | `platform:tenant:status` |
| `/org-type-templates` | 默认机构类型模板 | 类型、允许关系、发布模板版本 | `platform:org-template:manage` |
| `/permission-resources` | 权限资源 | 菜单/按钮/API/数据模块发布及废弃 | `platform:permission:manage` |
| `/platform-admins` | 平台身份 | 创建、停用、恢复码管理 | `platform:identity:manage` |
| `/audit` | 平台审计 | 分页查询、查看脱敏详情 | `platform:audit:view` |
| `/tenant-audit-access` | 租户安全审计查看 | 填原因、再认证后临时查看 | `platform:tenant-audit:view` |

创建租户使用分步表单：租户资料 → 根机构和机构业务编码 → 首位根机构所有者 → 安全策略初值 → 确认。提交后展示编排状态，不允许用户连续提交。

### 3.3 tenant-admin 页面

| 路由 | 页面 | 核心动作 | 权限 |
| --- | --- | --- | --- |
| `/login` | 租户身份登录 | 用户名密码或手机号验证码 | 匿名 |
| `/account-select` | 账号选择 | 展示租户、机构、用户名、最近登录 | 短期选择凭证 |
| `/dashboard` | 管理总览 | 本机构用户/角色及安全提醒 | `iam:dashboard:view` |
| `/organizations` | 机构树 | 搜索、分页加载子级、新增/编辑/迁移/停用 | `iam:org:view/manage` |
| `/organization-types` | 机构类型 | 类型及允许关系维护 | 仅根所有者且 `iam:org-type:manage` |
| `/users` | 用户列表 | 本机构分页查询、创建、停用、注销 | `iam:user:view/manage` |
| `/users/:id` | 用户详情 | 资料、角色、有效期、会话、安全历史 | 按子动作拆权限 |
| `/users/:id/reset` | 管理员重置 | 选择短信或一次性链接、再认证、原因 | `iam:user:password-reset` |
| `/owners` | 机构所有者 | 查看和受控转让 | `iam:org-owner:transfer` |
| `/roles` | 角色列表 | 新增、复制、停用、删除 | `iam:role:view/manage` |
| `/roles/:id/permissions` | 角色功能权限 | 只能分配操作者拥有且租户启用的权限 | `iam:role:grant` |
| `/roles/:id/data-scopes` | 角色数据范围 | 默认范围、模块覆盖、指定机构 | `iam:role:data-scope` |
| `/tenant/features` | 租户功能 | 停用/启用租户功能 | 根所有者且 `iam:tenant-feature:manage` |
| `/tenant/menus` | 租户菜单显示 | 隐藏/显示，父节点级联预览 | 根所有者且 `iam:tenant-menu:manage` |
| `/tenant/security` | 安全策略 | 并发设备、记住我、密码到期、审计保留 | 根所有者且 `iam:tenant-security:manage` |
| `/audit` | 租户审计 | 脱敏分页查询、异步导出申请 | `iam:audit:view` |
| `/profile` | 个人资料 | 显示名、用户名修改、手机号绑定 | 已登录 |
| `/security/sessions` | 设备会话 | 下线指定设备或其它全部设备 | 已登录 |

“机构树”不能一次加载整租户：根节点和子节点按需分页，搜索结果返回祖先面包屑。“角色数据范围”先展示操作者授权上限，超范围选项由后端返回不可分配原因，前端不自行推导最终边界。

### 3.4 tenant-portal 页面

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/login` | 登录 | 用户名密码、手机号验证码 |
| `/account-select` | 账号选择 | 多账号时使用一次性选择凭证 |
| `/activate` | 账号激活 | 校验一次性票据并设置初始密码 |
| `/reset-password` | 重置密码 | 自助或管理员发起后的受限流程 |
| `/dashboard` | 业务工作台 | 只承载已发布且有权的业务菜单 |
| `/profile` | 个人资料 | 展示名、用户名和手机号安全操作 |
| `/security/password` | 修改密码 | 当前密码/短信再认证后修改 |
| `/security/sessions` | 设备管理 | 查看和下线设备 |
| `/forbidden` | 无权限 | 展示 traceId 和返回入口 |

食堂、采购等业务模块尚未确认，当前只预留动态菜单挂载点，不创建假页面、假接口或假字段。

### 3.5 前端模块目录

三个应用按实际页面选取模块，示例：

```text
src/
├─ app/
│  ├─ router/guards.ts
│  ├─ store/session.ts
│  └─ bootstrap.ts
├─ modules/
│  ├─ auth/{api,components,pages,types}/
│  ├─ organization/{api,components,pages,types}/
│  ├─ user/{api,components,pages,types}/
│  ├─ role/{api,components,pages,types}/
│  ├─ permission/{api,components,pages,types}/
│  ├─ audit/{api,components,pages,types}/
│  └─ profile/{api,components,pages,types}/
└─ shared/
   ├─ http/client.ts
   ├─ feedback/
   ├─ auth/
   └─ components/
```

### 3.6 前端启动与鉴权流程

1. 加载本应用 Token，不读取其他应用 Token Key。
2. 调用 `GET /api/auth/v1/session` 获取会话身份、应用类型和版本摘要。
3. 调用 `GET /api/iam/v1/me/bootstrap` 获取用户资料、菜单、权限码和租户配置。
4. 校验返回的 `appCode` 与当前应用一致；不一致立即退出。
5. 动态注册后端已发布菜单对应的本地路由；未知组件键进入安全占位页并上报，不执行远端代码。
6. 收到 `AUTH_SESSION_INVALIDATED`、401 或版本失效码时清空本应用会话并跳转登录。

## 4. REST 契约规范

### 4.1 通用约定

- 外部版本前缀：`/api/auth/v1`、`/api/iam/v1`。
- 内部版本前缀：`/internal/auth/v1`、`/internal/iam/v1`，不得经 Gateway 暴露。
- JSON 使用 camelCase，时间使用 ISO-8601 带时区字符串，数据库时间统一 `timestamptz`。
- bigint ID 在前端统一用十进制字符串表示，避免 JavaScript 精度损失。
- 成功及失败均复用现有 `ApiResponse<T>`：`code/message/data/timestamp/traceId`。
- 列表请求使用 `pageNo`（从 1 开始）、`pageSize`（默认 20，最大 100）；禁止无分页列表。
- 分页响应统一规划为 `PageResult<T>{items,pageNo,pageSize,total}`。深分页审计/事件接口改用游标。
- 创建成功返回 201，查询/更新成功返回 200，无响应删除返回 200 的统一响应；400 参数或业务错误、401 未登录、403 无权、404 资源不存在、409 状态/唯一性冲突、429 限流。
- 更新接口必须携带 `version` 做乐观锁；冲突返回 409，禁止静默覆盖。
- 敏感操作额外传 `reason` 和 `reauthTicket`；不得重复提交密码或验证码。
- `Idempotency-Key` 用于租户创建、用户创建、生成激活/重置票据、所有权转让等命令，服务端保存结果摘要并设置期限。

### 4.2 核心公共 VO

```text
SessionVO
  tokenName, tokenValue, sessionId, appCode, identityType,
  accountId, tenantId?, organizationId?, idleExpiresAt, absoluteExpiresAt

PageResult<T>
  items, pageNo, pageSize, total

ReauthTicketVO
  ticket, expiresAt, allowedAction

LoginResultVO
  nextStep: AUTHENTICATED | ACCOUNT_SELECTION_REQUIRED | SECOND_FACTOR_REQUIRED
  session?: SessionVO
  selectionTicket?: string
  accounts?: AccountOptionVO[]
  secondFactorTicket?: string
```

票据都是高熵一次性随机值，数据库或 Redis 只保存摘要；响应只展示一次。

## 5. Auth 外部接口

### 5.1 登录与会话

| 方法与路径 | 入参摘要 | 返回 | 说明 |
| --- | --- | --- | --- |
| `POST /api/auth/v1/login/password` | `appCode, username, password, rememberMe, device, captcha?` | `LoginResultVO` | 当前平台和租户身份在密码验证成功后直接登录；未来高风险场景可返回二次验证步骤 |
| `POST /api/auth/v1/sms/challenges` | `purpose, mobile, captchaTicket?` | `challengeId, maskedMobile, expiresAt, resendAt` | 响应不泄露绑定账号 |
| `POST /api/auth/v1/login/sms` | `appCode, challengeId, code, rememberMe, device` | `LoginResultVO` | 一账号直接登录，多账号返回选择项 |
| `POST /api/auth/v1/login/account-selection` | `selectionTicket, accountId, rememberMe, device` | `SessionVO` | 票据一次性，账号必须属于已验证手机号候选集 |
| `POST /api/auth/v1/login/platform-second-factor` | `secondFactorTicket, challengeId, code, device` | `SessionVO` | 预留给异常设备或高风险登录的手机验证码步骤，当前普通登录不调用 |
| `POST /api/auth/v1/login/platform-recovery-code` | `secondFactorTicket, recoveryCode` | `SessionVO` | 预留给受控恢复或高风险流程；普通登录不得消费恢复码 |
| `GET /api/auth/v1/session` | 无 | `SessionVO` | 当前应用启动校验 |
| `POST /api/auth/v1/logout` | 无 | `Void` | 当前设备退出 |
| `GET /api/auth/v1/sessions` | 分页 | `PageResult<DeviceSessionVO>` | 当前账号设备 |
| `DELETE /api/auth/v1/sessions/{sessionId}` | `version` | `Void` | 下线指定设备 |
| `POST /api/auth/v1/sessions/actions/logout-others` | 无 | `Void` | 当前设备以外全部下线 |

`device` 只接受服务端允许的 `deviceId/deviceType/deviceName/userAgentSummary`；IP 从可信代理链解析，不能由客户端传入。

### 5.2 激活、密码和手机号

| 方法与路径 | 用途 |
| --- | --- |
| `GET /api/auth/v1/activations/{ticket}/context` | 返回脱敏账号上下文和票据状态 |
| `POST /api/auth/v1/activations/{ticket}/complete` | 设置初始密码并激活 |
| `POST /api/auth/v1/password/forgot/challenges` | 发起已验证手机号自助找回 |
| `POST /api/auth/v1/password/forgot/verify` | 校验验证码并返回一次性改密票据 |
| `POST /api/auth/v1/password/reset/complete` | 使用自助或管理员重置票据设置新密码 |
| `POST /api/auth/v1/password/change` | 当前密码或再认证票据修改密码 |
| `POST /api/auth/v1/reauth/password` | 管理员输入当前密码获得短期再认证票据 |
| `POST /api/auth/v1/reauth/sms` | 已验证手机二次验证获得再认证票据 |
| `POST /api/auth/v1/mobile/bind/challenges` | 给新手机号发绑定验证码 |
| `POST /api/auth/v1/mobile/bind/confirm` | 正式绑定已验证手机号并下线其它会话 |
| `POST /api/auth/v1/mobile/change/confirm` | 完成新旧凭证验证后的换绑 |
| `POST /api/auth/v1/platform/recovery/complete` | 忘记密码时使用恢复码进入专用改密流程 |

管理员生成激活或重置凭证的入口属于 IAM 管理命令；IAM 完成权限校验和审计后，通过内部 Client 让 Auth 生成秘密票据。

## 6. IAM 外部接口

### 6.1 当前身份

| 方法与路径 | 返回 |
| --- | --- |
| `GET /api/iam/v1/me/bootstrap` | 资料、租户/机构、角色摘要、菜单树、功能权限、租户功能和版本 |
| `PATCH /api/iam/v1/me/profile` | 修改显示名称 |
| `POST /api/iam/v1/me/username/actions/change` | `newUsername, reason, reauthTicket, version` |
| `PUT /api/iam/v1/me/menu-preferences/{menuId}` | 设置当前用户隐藏状态 |

### 6.2 平台管理

| 方法与路径 | 说明 |
| --- | --- |
| `GET/POST /api/iam/v1/platform/tenants` | 分页查询、创建租户 |
| `GET/PATCH /api/iam/v1/platform/tenants/{tenantId}` | 详情、非状态资料更新 |
| `POST /api/iam/v1/platform/tenants/{tenantId}/actions/{suspend|resume|expire|cancel}` | 生命周期命令 |
| `GET/POST /api/iam/v1/platform/org-type-templates` | 模板版本和类型 |
| `PUT /api/iam/v1/platform/org-type-templates/{id}/relations` | 保存并校验 DAG |
| `GET/POST /api/iam/v1/platform/permission-resources` | 权限资源分页、草稿创建 |
| `POST /api/iam/v1/platform/permission-resources/{id}/actions/publish` | 发布并永久占用权限码 |
| `POST /api/iam/v1/platform/permission-resources/{id}/actions/deprecate` | 废弃但不复用 |
| `GET/POST /api/iam/v1/platform/identities` | 平台身份管理 |
| `POST /api/iam/v1/platform/identities/{id}/recovery-codes` | 再认证、原因、生成新恢复码组 |
| `GET /api/iam/v1/platform/audit-logs` | 平台审计分页 |
| `POST /api/iam/v1/platform/tenant-audit-access-grants` | 临时查看租户审计授权 |

租户创建命令同步创建 IAM 数据；Auth 凭证初始化采用有状态编排：`INITIALIZING -> ACTIVE/PROVISION_FAILED`。失败时租户不可登录，支持按同一幂等键安全重试，禁止留下“看似成功但不能激活”的租户。

### 6.3 租户、机构和所有者

| 方法与路径 | 说明 |
| --- | --- |
| `GET/PATCH /api/iam/v1/tenant/settings` | 租户可维护资料 |
| `GET/PUT /api/iam/v1/tenant/security-policy` | 会话、密码、审计保留策略 |
| `GET/PUT /api/iam/v1/tenant/features` | 功能启停，敏感操作 |
| `GET/PUT /api/iam/v1/tenant/menu-visibility` | 菜单隐藏，父节点自动级联计算 |
| `GET/POST /api/iam/v1/tenant/organization-types` | 机构类型分页/创建 |
| `PATCH /api/iam/v1/tenant/organization-types/{id}` | 修改名称、顺序、状态 |
| `PUT /api/iam/v1/tenant/organization-type-relations` | 全量提交关系版本并校验 DAG |
| `GET /api/iam/v1/tenant/organizations` | 根/搜索分页，支持 `parentId` |
| `POST /api/iam/v1/tenant/organizations` | 新建机构和受保护所有者角色 |
| `GET/PATCH /api/iam/v1/tenant/organizations/{id}` | 详情、资料或合法类型变更 |
| `POST /api/iam/v1/tenant/organizations/{id}/actions/move` | 同租户迁移，校验整棵子树 |
| `POST /api/iam/v1/tenant/organizations/{id}/actions/{disable|enable|delete}` | 状态命令 |
| `GET /api/iam/v1/tenant/organizations/{id}/owner` | 当前所有者 |
| `POST /api/iam/v1/tenant/organizations/{id}/owner/actions/transfer` | 单事务受控转让 |

删除机构前由 IAM 校验无用户、无角色、无下级机构；是否存在其他业务数据必须由各数据所有者服务提供幂等占用检查，未接入检查的已使用机构不得删除。

### 6.4 用户

| 方法与路径 | 说明 |
| --- | --- |
| `GET/POST /api/iam/v1/tenant/users` | 权限范围内分页查询、创建待激活账号 |
| `GET/PATCH /api/iam/v1/tenant/users/{accountId}` | 详情、显示名/工号/有效期更新 |
| `PUT /api/iam/v1/tenant/users/{accountId}/roles` | 替换角色集合并使全部会话失效 |
| `POST /api/iam/v1/tenant/users/{accountId}/actions/{disable|enable|cancel}` | 状态命令 |
| `POST /api/iam/v1/tenant/users/{accountId}/activation-links` | 生成一次性 24 小时激活链接 |
| `POST /api/iam/v1/tenant/users/{accountId}/password-reset` | 选择 `SMS` 或 `ONE_TIME_LINK` |
| `GET /api/iam/v1/tenant/users/{accountId}/security-events` | 脱敏安全事件分页 |

创建用户 DTO 至少包含已确认字段：`username, displayName?, employeeNumber?, organizationId, roleIds, validityMode, effectiveAt?, expiresAt?, pendingMobile?`。`organizationId` 创建后禁止修改。创建时服务端校验至少一个有效角色、角色同机构、授权不越权。

### 6.5 角色、权限和数据范围

| 方法与路径 | 说明 |
| --- | --- |
| `GET/POST /api/iam/v1/tenant/roles` | 本机构角色分页、创建 |
| `GET/PATCH /api/iam/v1/tenant/roles/{roleId}` | 详情、名称和说明更新 |
| `POST /api/iam/v1/tenant/roles/{roleId}/actions/{disable|enable|delete}` | 角色状态命令 |
| `GET/PUT /api/iam/v1/tenant/roles/{roleId}/permissions` | 查询/替换功能权限集合 |
| `GET/PUT /api/iam/v1/tenant/roles/{roleId}/data-policy` | 默认范围和模块覆盖 |
| `GET /api/iam/v1/tenant/grant-boundary` | 返回操作者当前可授予上限 |
| `GET /api/iam/v1/tenant/permission-tree` | 租户启用且操作者可分配的权限树 |
| `GET /api/iam/v1/tenant/data-modules` | 已发布数据权限模块 |
| `GET /api/iam/v1/tenant/audit-logs` | 租户内审计分页 |

角色写操作必须锁定并校验 `version`，比较操作者授权上限，在同一事务更新角色/关系/版本/审计/Outbox。不得由前端传入“我是所有者”等可信标志。

## 7. 内部 API 和跨服务调用

| 调用方向 | 方法与路径 | 用途 |
| --- | --- | --- |
| Auth → IAM | `POST /internal/iam/v1/login-resolutions/username` | 用归一化用户名解析登录身份和状态 |
| Auth → IAM | `POST /internal/iam/v1/login-resolutions/mobile-accounts` | 验证短信后批量获取可登录候选账号 |
| Auth → IAM | `GET /internal/iam/v1/accounts/{id}/auth-snapshot` | 登录前获取租户、机构、角色和版本 |
| Auth → IAM | `POST /internal/iam/v1/accounts/{id}/login-records` | 幂等更新最近登录摘要 |
| IAM → Auth | `POST /internal/auth/v1/accounts/{id}/provision` | 初始化凭证容器和激活方式 |
| IAM → Auth | `POST /internal/auth/v1/accounts/{id}/activation-tickets` | 生成激活票据 |
| IAM → Auth | `POST /internal/auth/v1/accounts/{id}/password-resets` | 进入待重置并生成流程凭证 |
| IAM → Auth | `POST /internal/auth/v1/security-events` | 消费账号/角色/机构/租户失效事件 |
| IAM → Auth | `POST /internal/auth/v1/platform-identities/{id}/provision` | 初始化平台凭证 |

内部 DTO 使用独立 `v1` 包，不能复用 Entity。Client 必须配置连接/读取超时；GET 和带事件 ID 的幂等命令可有限重试，其他写请求不得自动重试。

## 8. 权限资源设计

### 8.1 资源模型

权限资源类型：`DIRECTORY`、`MENU`、`BUTTON`、`API`。菜单组件键只映射前端本地组件；API 资源记录 HTTP 方法和模板路径；一个业务按钮权限可以关联多个 API 资源。`DATA_MODULE` 保留为历史数据库兼容枚举，不在通用权限资源页面创建或筛选；数据权限模块统一通过独立的 `iam_data_module` 发布流程维护。

权限码格式固定为小写三段或四段：`域:资源:动作`，例如 `iam:user:view`。已发布码永久保留，废弃后不可复用。

首批 IAM 权限码建议在首个迁移中固化：

```text
platform:dashboard:view
platform:tenant:view | create | update | status
platform:org-template:manage
platform:permission:manage
platform:identity:manage
platform:audit:view
platform:tenant-audit:view

iam:dashboard:view
iam:org:view | create | update | move | status | delete
iam:org-type:view | manage
iam:org-owner:view | transfer
iam:user:view | create | update | status | cancel
iam:user:role-assign
iam:user:password-reset
iam:role:view | create | update | status | delete
iam:role:grant
iam:role:data-scope
iam:tenant-feature:manage
iam:tenant-menu:manage
iam:tenant-security:manage
iam:audit:view | export
```

这些是**待发布的新权限契约**。编码前生成正式初始化迁移并评审含义；发布后只允许废弃，不允许改义或复用。机构所有者通过系统保护身份动态获得租户已启用的全部 IAM 功能，不通过普通角色人工勾选所有者权限。

### 8.2 数据范围枚举

```text
SELF
CURRENT_ORG
CURRENT_ORG_AND_DESCENDANTS
SPECIFIED_ORGS
SPECIFIED_ORGS_AND_DESCENDANTS
TENANT_ALL
```

每个数据模块发布 `moduleCode`、所属服务、数据归属字段语义和支持范围。未确认真实归属字段的模块不得发布。`created_by` 只是常见默认，不是所有模块的强制猜测。

## 9. 错误码设计

保留现有 `0`、`COMMON_400`、`AUTH_401`、`AUTH_403`、`COMMON_500`。新增码按领域稳定发布，HTTP 状态与业务码同时表达：

| 错误码 | HTTP | 含义 |
| --- | --- | --- |
| `AUTH_1001` | 400 | 用户名或密码错误（不泄露账号存在性） |
| `AUTH_1002` | 423 | 密码登录临时锁定 |
| `AUTH_1003` | 400 | 需要图形验证码 |
| `AUTH_1004` | 400 | 短信验证码无效或已失效 |
| `AUTH_1005` | 429 | 发送或验证过于频繁 |
| `AUTH_1006` | 401 | 会话已因安全变化失效 |
| `AUTH_1007` | 400 | 一次性票据无效或已使用 |
| `AUTH_1008` | 403 | 当前应用入口不匹配 |
| `IAM_2001` | 409 | 用户名已被当前或历史账号占用 |
| `IAM_2002` | 409 | 工号已在机构内永久占用 |
| `IAM_2003` | 409 | 机构业务编码已占用 |
| `IAM_2004` | 400 | 机构父子类型关系不允许 |
| `IAM_2005` | 409 | 机构迁移会形成环或破坏子树约束 |
| `IAM_2006` | 409 | 资源版本冲突，请刷新后重试 |
| `IAM_2007` | 403 | 分配权限超出操作者授权上限 |
| `IAM_2008` | 403 | 分配数据范围超出操作者范围 |
| `IAM_2009` | 409 | 机构所有者受保护或唯一性冲突 |
| `IAM_2010` | 409 | 已使用机构/类型只能停用 |
| `IAM_2011` | 403 | 租户、机构或账号当前不可用 |
| `IAM_2012` | 400 | 账号至少需要一个有效角色 |
| `INTERNAL_3001` | 401 | 内部 HMAC 签名无效 |
| `INTERNAL_3002` | 409 | Nonce 或事件重复 |

枚举类、前端提示映射和 API 文档必须来自同一契约；对外提示不得包含数据库、签名或账号枚举细节。

## 10. IAM 数据库设计

### 10.1 通用规范

- 数据库建议逻辑名 `smile_iam`，真实名称和账号从环境变量提供。
- 主键统一 `bigint` + 每表 PostgreSQL sequence，接口序列化为字符串。
- 核心可变表包含 `created_by bigint`、`created_time timestamptz`、`updated_by bigint`、`updated_time timestamptz`、`is_deleted boolean default false`、`version bigint default 0`。下列表格为突出领域字段而省略这些公共列；关联表也必须至少保留创建人与创建时间，只有明确标注“只追加/短期凭证”的表按其生命周期设计。
- 状态和类型使用 `varchar` + 应用枚举/必要 CHECK，不使用难演进的 PostgreSQL ENUM。
- 所有唯一约束都要考虑软删除和“永久保留”规则；永久标识放注册表，不依赖软删除唯一索引。
- 禁止 `SELECT *`；Mapper 显式列出字段。

### 10.2 平台和租户

| 表 | 关键字段 | 约束/索引 |
| --- | --- | --- |
| `iam_platform_identity` | `id, username, normalized_username, display_name, status, authz_version` | `normalized_username` 全局唯一；与租户账号注册表统一检查 |
| `iam_tenant` | `id, tenant_code, name, status, root_organization_id, security_version, template_version, provision_status` | `tenant_code` 永久唯一；索引 `status` |
| `iam_tenant_code_registry` | `id, tenant_code, tenant_id, reserved_time` | `tenant_code` 和 `tenant_id` 永久唯一；不设置租户外键，异常清理租户后仍保留标识 |
| `iam_tenant_security_policy` | `tenant_id, concurrent_login_enabled, max_devices, remember_me_enabled, idle_seconds, absolute_seconds, remember_idle_seconds, remember_absolute_seconds, password_expiry_enabled, password_expiry_days, audit_retention_days` | `tenant_id` 唯一；数值受平台上下限 CHECK |
| `iam_admin_region` | `id, parent_id, region_code, name, level_code, status` | `region_code` 唯一；仅作关联引用 |

平台用户名和租户用户名必须共用一个全局用户名注册聚合。推荐让 `iam_username_registry.subject_type` 同时支持 `PLATFORM_IDENTITY` 和 `TENANT_ACCOUNT`，`iam_platform_identity` 不单独承担永久保留逻辑。

### 10.3 机构类型和机构树

| 表 | 关键字段 | 约束/索引 |
| --- | --- | --- |
| `iam_org_type_template` | `id, template_version, type_code, name, sort_order, status` | `(template_version,type_code)` 唯一 |
| `iam_org_type_template_relation` | `template_version, parent_type_code, child_type_code` | 组合唯一；发布前校验 DAG |
| `iam_org_type` | `id, tenant_id, type_code, name, sort_order, status, source_template_version` | `(tenant_id,type_code)` 永久唯一 |
| `iam_org_type_relation` | `id, tenant_id, parent_type_id, child_type_id` | 三列组合唯一；禁止自环，Service 校验 DAG |
| `iam_organization` | `id, tenant_id, parent_id, org_type_id, business_code, name, admin_region_id?, own_status, path_version` | `(tenant_id,parent_id,normalized_name)` 有效记录唯一；索引 `(tenant_id,parent_id,own_status)` |
| `iam_organization_closure` | `tenant_id, ancestor_id, descendant_id, depth` | 主键三元组；索引 `(tenant_id,descendant_id,depth)` 和 `(tenant_id,ancestor_id,depth)` |
| `iam_org_code_registry` | `tenant_id, normalized_code, organization_id, reserved_time` | `(tenant_id,normalized_code)` 永久唯一，不物理删除 |
| `iam_org_name_history` | `id, tenant_id, organization_id, old_name, new_name, changed_by, changed_time` | 索引 `(tenant_id,organization_id,changed_time desc)` |
| `iam_org_owner` | `organization_id, tenant_id, account_id, protected_role_id, effective_time` | `organization_id` 唯一，`account_id` 可按业务查询 |
| `iam_org_owner_history` | `id, tenant_id, organization_id, from_account_id, to_account_id, reason, operator_id, changed_time` | 只追加 |

Closure 表包含每个节点到自身的 `depth=0` 记录。新增节点插入父节点全部祖先；迁移子树在单个 Service 事务中锁定相关机构、校验类型关系和环，再批量重建跨边界闭包。任何查询都强制 `tenant_id`。

“实际停用”不冗余写回所有子级：查询账号或机构时，通过 closure 判断是否存在停用祖先。高频登录可把结果放短 TTL 快照，并由 `path_version/security_version` 保证失效。

### 10.4 账号、用户名和工号

| 表 | 关键字段 | 约束/索引 |
| --- | --- | --- |
| `iam_account` | `id, tenant_id, organization_id, username, normalized_username, display_name?, employee_number?, status, validity_mode, effective_at?, expires_at?, authz_version, profile_version` | 索引 `(tenant_id,organization_id,status,id)`；禁止更新 `organization_id` |
| `iam_username_registry` | `normalized_username, subject_type, subject_id, original_username, login_enabled, reserved_time, disabled_time?` | `normalized_username` 主键/永久保留；每个 subject 仅一个 `login_enabled=true` |
| `iam_employee_number_registry` | `tenant_id, organization_id, normalized_employee_number, account_id, reserved_time` | 三列永久唯一，不因注销删除 |
| `iam_account_role` | `tenant_id, account_id, role_id, assigned_by, assigned_time` | `(account_id,role_id)` 唯一；校验同租户同机构 |

用户名归一化统一使用服务端规则：去首尾空白、Unicode 规范化、`Locale.ROOT` 小写；首版允许字符和长度必须在接口 DTO 与数据库 CHECK 同步。修改用户名在同一事务中把旧注册项 `login_enabled=false`、插入新注册项、更新账号和授权版本、写审计和 Outbox。

### 10.5 角色、权限、菜单和数据范围

| 表 | 关键字段 | 约束/索引 |
| --- | --- | --- |
| `iam_role` | `id, tenant_id, organization_id, role_code, name, normalized_name, description, role_type, status, authz_version` | `role_code` 永久唯一；当前有效 `(organization_id,normalized_name)` 唯一；`OWNER` 受保护 |
| `iam_permission_resource` | `id, permission_code, resource_type, parent_id?, name, description, app_code, route_path?, component_key?, api_method?, api_path_pattern?, feature_code?, publish_status, semantic_version` | `permission_code` 永久唯一；已发布只可废弃 |
| `iam_permission_api_binding` | `permission_id, api_resource_id` | `(permission_id,api_resource_id)` 唯一；把一个可授予业务权限绑定到一个或多个 API 资源 |
| `iam_role_permission` | `tenant_id, role_id, permission_id, granted_by, granted_time` | `(role_id,permission_id)` 唯一 |
| `iam_tenant_feature` | `tenant_id, feature_code, enabled, reason?, changed_by` | `(tenant_id,feature_code)` 唯一 |
| `iam_tenant_menu_config` | `tenant_id, menu_permission_id, hidden, changed_by` | `(tenant_id,menu_permission_id)` 唯一；读取时计算父级隐藏 |
| `iam_account_menu_preference` | `account_id, menu_permission_id, hidden` | 组合唯一，只影响显示 |
| `iam_data_module` | `id, module_code, service_code, name, ownership_semantics, supported_scopes, publish_status` | `module_code` 永久唯一 |
| `iam_role_data_policy` | `id, tenant_id, role_id, module_code, scope_type` | `(role_id,module_code)` 唯一；`module_code='*'` 表示默认范围 |
| `iam_role_data_scope_org` | `policy_id, tenant_id, organization_id` | `(policy_id,organization_id)` 唯一；仅指定机构范围使用 |

角色删除采用软删除且永久保留 ID/roleCode，名称可以被新角色使用。每次权限或数据范围变更都提升角色 `authz_version`，计算受影响账号并写一条可批量消费的失效事件；不在事务内循环删除 Redis Key。

### 10.6 审计、事件和幂等

| 表 | 关键字段 | 说明 |
| --- | --- | --- |
| `iam_audit_log` | `id, tenant_id?, operator_type, operator_id, operator_org_id?, action_code, target_type, target_id, reason?, result, masked_diff_json, ip_hash?, device_summary?, trace_id, occurred_time` | 只追加；按月分区可在容量达到阈值后启用 |
| `iam_outbox_event` | `id, event_id, aggregate_type, aggregate_id, event_type, payload_json, status, retry_count, next_retry_time, last_error_code?, created_time, published_time?` | `event_id` 唯一；索引 `(status,next_retry_time,id)` |
| `iam_idempotency_record` | `idempotency_key_hash, operator_id, operation_code, request_hash, response_ref, status, expires_at` | 同键不同请求摘要返回冲突 |
| `iam_sensitive_action_ticket` | `ticket_hash, operator_id, action_code, expires_at, used_time?` | 如统一由 Auth 发票据，IAM 只保存消费引用 |

审计差异 JSON 只保存字段白名单和脱敏值，绝不保存密码、验证码、Token、完整手机号、HMAC 密钥或完整一次性链接。

## 11. Auth 数据库设计

数据库建议逻辑名 `smile_auth`，真实配置来自环境。Auth 表中的 `subject_type + subject_id` 区分平台身份和租户账号。

| 表 | 关键字段 | 约束/索引 |
| --- | --- | --- |
| `auth_credential` | `id, subject_type, subject_id, password_hash, algorithm, password_changed_at, credential_version, status` | `(subject_type,subject_id)` 唯一 |
| `auth_password_history` | `id, subject_type, subject_id, password_hash, algorithm, changed_time` | 索引 subject + time；只保留策略要求数量及审计期限 |
| `auth_mobile_binding` | `id, subject_type, subject_id, mobile_ciphertext, mobile_hash, masked_mobile, verified_time, status` | 账号当前有效绑定唯一；`mobile_hash` 非唯一并建查询索引 |
| `auth_sms_challenge` | `id, challenge_id, purpose, mobile_hash, code_hash, attempts, status, expires_at, consumed_at?` | `challenge_id` 唯一；定期清理过期记录 |
| `auth_account_selector_ticket` | `id, ticket_hash, mobile_hash, candidate_digest, app_code, expires_at, consumed_at?` | 票据唯一，一次消费 |
| `auth_activation_ticket` | `id, subject_id, ticket_hash, expires_at, consumed_at?, superseded_at?` | 每账号只有最新票据有效 |
| `auth_password_reset_ticket` | `id, subject_id, mode, ticket_hash?, challenge_id?, expires_at, consumed_at?, superseded_at?` | 一次使用；只存摘要 |
| `auth_platform_recovery_code` | `id, platform_identity_id, batch_id, code_hash, consumed_at?` | 每码一次；新批次废弃旧批次 |
| `auth_login_failure` | `subject_key_hash, ip_hash, device_hash, password_failures, captcha_required, locked_until, updated_time` | 多维限流，禁止明文敏感标识 |
| `auth_device_session` | `id, session_id, subject_type, subject_id, tenant_id?, organization_id?, app_code, token_digest, device_id_hash, device_type, device_name, login_method, login_ip_masked, login_time, last_active_time, idle_expires_at, absolute_expires_at, status, snapshot_version` | `session_id` 唯一；账号/状态/到期索引 |
| `auth_permission_snapshot` | `id, session_id, snapshot_version, signed_payload, user_authz_version, role_version_digest, org_path_version, tenant_security_version, expires_at` | 每会话当前版本唯一 |
| `auth_consumed_event` | `event_id, event_type, consumed_time, result` | `event_id` 主键，消费幂等 |
| `auth_audit_log` | 与 IAM 审计相同基础字段，另含 `login_method, failure_reason_code` | 只追加、敏感字段脱敏 |

手机号使用可轮换的数据加密保存密文，同时保存带服务端 Pepper 的 HMAC 查询摘要；Pepper 和加密密钥只来自密钥服务/环境变量。密码统一使用 Argon2id；当前 `ARGON2ID_V1` 参数为 16 字节盐、32 字节摘要、并行度 1、64 MiB 内存和 3 次迭代。参数升级必须发布新算法版本并在成功登录后渐进迁移，禁止直接覆盖旧算法语义。

Sa-Token 的 Redis 会话是运行态，`auth_device_session` 是可审计会话索引；登录、续期、下线必须保持幂等。数据库状态为强制失效依据，Redis 用于高频快速判断。

## 12. Redis Key 和 TTL

复用 `RedisKeyBuilder`，前缀由配置提供，示例按 `canteen-smile:<service>:<business>:<id>`。以下为本方案新确立的 Key 契约：

| Key 模板 | TTL | 用途 |
| --- | --- | --- |
| `canteen-smile:auth:sms:challenge:{challengeId}` | 5 分钟 | 验证码挑战状态 |
| `canteen-smile:auth:sms:send-lock:{mobileHash}` | 60 秒 | 防重复发送 |
| `canteen-smile:auth:sms:hour-limit:{mobileHash}` | 2 小时 | 小时窗口限流，具体次数待配置 |
| `canteen-smile:auth:login:failure:{subjectHash}` | 锁定结束后至少 15 分钟 | 失败计数和锁定 |
| `canteen-smile:auth:captcha:{ticketId}` | 5 分钟 | 图形验证码挑战 |
| `canteen-smile:auth:selector:{ticketHash}` | 5 分钟 | 短信登录账号选择 |
| `canteen-smile:auth:reauth:{ticketHash}` | 5 分钟 | 敏感操作再认证 |
| `canteen-smile:auth:platform-second-factor:{ticketHash}` | 5 分钟 | 平台密码校验成功后的二次验证上下文；原子获取并删除 |
| `canteen-smile:auth:idempotency:{keyHash}` | 24 小时 | 高风险命令短期幂等加速 |
| `canteen-smile:internal:hmac:nonce:{caller}:{nonce}` | 10 分钟 | 防重放，SET NX |
| `canteen-smile:iam:tenant-version:{tenantId}` | 15 分钟 | 租户安全版本快照 |
| `canteen-smile:iam:account-version:{accountId}` | 15 分钟 | 用户授权版本快照 |
| `canteen-smile:iam:permission-snapshot:{sessionId}` | 不超过会话空闲期限，建议 15 分钟刷新 | 已签名权限快照 |

Sa-Token 自身 Key 继续由其配置管理，不手工拼接或删除。所有非永久 Key 显式设置 TTL；批量失效通过版本比较，不通过扫描 Redis Cluster Key。

## 13. HMAC 内部签名

本方案建议将以下规则作为 v1 固定契约：

- 算法：`HMAC-SHA256`；请求体摘要：`SHA-256` 小写十六进制。
- 请求头：`X-Caller-Id`、`X-Key-Id`、`X-Timestamp`、`X-Nonce`、`X-Event-Id`、`X-Content-SHA256`、`X-Signature`。
- 规范串按换行连接：HTTP 方法、规范化路径、排序后的查询串、内容摘要、时间戳、Nonce、调用方、事件 ID。
- 服务端允许时钟偏差默认 ±5 分钟；Nonce 保存 10 分钟并使用 Redis `SET NX`。
- 密钥按 `caller + keyId` 查找，支持当前/下一把密钥并行轮换；密钥不得进入 YAML、日志、数据库或事件。
- 签名校验在读取业务 DTO 和权限处理之前完成；失败只返回稳定错误码并记录脱敏安全审计。

内部网络白名单与 HMAC 缺一不可。生产环境应使用 TLS；HMAC 不能替代传输加密。

## 14. 权限快照和业务服务校验

权限快照至少包含：

```text
identityType, appCode, accountId, tenantId, organizationId,
roleIds, permissionCodes,
dataPolicies[moduleCode],
userAuthzVersion, roleVersionDigest,
orgPathVersion, tenantSecurityVersion,
issuedAt, expiresAt, signature
```

每个请求的最终逻辑：

1. Sa-Token 验证会话和应用入口。
2. 比较租户、账号和授权版本；版本不一致返回 `AUTH_1006`。
3. 校验接口所需已发布权限码及租户功能状态。
4. 解析当前业务模块数据范围，不接受前端自报范围。
5. Mapper 将 `tenant_id = 当前租户` 和范围条件写入 SQL。
6. 详情、修改、删除同样带数据范围条件，不能先按 ID 查出后再只在 Java 判断。

业务服务不能跨库查询 IAM 的 `iam_organization_closure`。每个需要组织范围的业务服务应消费 `ORGANIZATION_PATH_CHANGED`，在自己的数据库维护只含组织 ID、祖先/后代关系和路径版本的 `security_org_closure_projection`；它不是 IAM Entity 的复制，也不包含机构资料。以已经确认同时具有 `tenant_id`、`organization_id`、`created_by` 的某业务模块为例：

```sql
WHERE business.tenant_id = :tenantId
  AND (
       business.created_by = :accountId
       OR business.organization_id IN (:exactOrganizationIds)
       OR EXISTS (
           SELECT 1
           FROM security_org_closure_projection scope_org
           WHERE scope_org.tenant_id = business.tenant_id
             AND scope_org.descendant_org_id = business.organization_id
             AND scope_org.ancestor_org_id IN (:ancestorOrganizationIds)
             AND scope_org.path_version = :orgPathVersion
       )
  )
```

示例中的集合必须由 MyBatis 参数化 `<foreach>` 或等价类型处理器绑定，禁止字符串拼接；空集合必须生成恒假分支，不能移除权限条件。登录/刷新时 IAM 签发范围快照；范围过大时依赖服务本地组织投影，不能在每次查询中跨服务远程调用。具体模块接入前必须明确 `tenant_id`、组织归属字段、本人字段和索引。

## 15. 关键流程

### 15.1 手机号登录多账号选择

```text
发送验证码 -> 校验验证码成功 -> Auth 调 IAM 批量解析候选账号
    ├─ 0 个：统一提示不可登录
    ├─ 1 个：校验版本并签发该应用会话
    └─ 多个：生成 5 分钟一次性 selectionTicket
               -> 用户选择 accountId
               -> Auth 校验候选摘要、消费票据、签发会话
```

候选项只包含账号选择必需的脱敏信息：`accountId, username, displayName, tenantName, organizationName, organizationPathSummary, lastLoginAt, isMostRecent`。

### 15.2 创建和激活账号

1. IAM 校验机构、角色、授权边界、用户名/工号永久注册表。
2. IAM 事务创建待激活账号、所有者保护角色、默认数据范围、审计和 Outbox。
3. IAM 通过 HMAC Client 幂等调用 Auth provision 创建 `PENDING` 凭证容器；成功后将租户编排状态置为 `ACTIVE`，失败则置为 `PROVISION_FAILED` 并由同一 Outbox 事件重试。接入消息队列后保持事件语义不变。
4. 用户通过短信或一次性 24 小时链接设置密码。
5. Auth 写凭证后用事件通知 IAM 激活；IAM 再校验有效期、机构和租户状态后转正常。
6. 任一重复回调按 eventId 幂等。

### 15.3 管理员重置密码

1. IAM 校验同机构/被授权下级范围、目标身份级别和 `iam:user:password-reset`。
2. 操作者填写原因并用当前密码或已验证手机获得 `reauthTicket`。
3. IAM 把账号置为待重置、提升版本、写审计和 Outbox。
4. Auth 收到事件后下线全部设备并生成短信流程或 30 分钟一次性链接。
5. 链接只返回当前请求一次，普通日志和审计只记票据摘要 ID。
6. 目标用户设置新密码；成功后恢复正常并再次提升凭证版本。

### 15.4 角色和租户状态变更

角色权限、数据范围、角色状态或用户角色关系改变时，IAM 在同一事务提升角色/用户版本并写 `AUTHORIZATION_CHANGED`。租户暂停/到期/注销时提升 `tenantSecurityVersion`，Auth 消费事件使租户全部设备逻辑失效；重新登录也必须实时查询或验证租户状态，不能因事件暂时失败而放行。

## 16. Outbox 事件

首批事件类型：

```text
ACCOUNT_PROVISION_REQUESTED
ACCOUNT_STATUS_CHANGED
ACCOUNT_USERNAME_CHANGED
ACCOUNT_VALIDITY_CHANGED
ACCOUNT_ROLES_CHANGED
ROLE_AUTHORIZATION_CHANGED
ORGANIZATION_STATUS_CHANGED
ORGANIZATION_PATH_CHANGED
TENANT_SECURITY_VERSION_CHANGED
PASSWORD_RESET_REQUESTED
MOBILE_BINDING_CHANGED
SESSION_INVALIDATION_REQUESTED
```

事件信封：`eventId, eventType, aggregateType, aggregateId, tenantId?, occurredAt, schemaVersion, traceId, payload`。Payload 只带消费者完成动作所需 ID、版本和状态，不带 Entity 全量快照或秘密。

XXL-JOB 每批按 `(status,next_retry_time,id)` 取有限数量并使用 `FOR UPDATE SKIP LOCKED`，指数退避加随机抖动；达到最大次数转 `DEAD` 并告警。批次、最大次数和告警渠道属于部署配置，不写死。接入消息队列后仍由 Outbox 发布，消费者继续按 eventId 幂等。

## 17. 配置与环境

每个服务保留 `application.yml`、`application-dev.yml`、`application-test.yml`、`application-prod.yml`，本机真实值放不提交 Git 的 `application-local.yml`。

建议环境变量分类：

```text
IAM_SERVER_PORT, IAM_DATASOURCE_URL, IAM_DATASOURCE_USERNAME, IAM_DATASOURCE_PASSWORD
AUTH_SERVER_PORT, AUTH_DATASOURCE_URL, AUTH_DATASOURCE_USERNAME, AUTH_DATASOURCE_PASSWORD
IAM_SERVICE_URI, AUTH_SERVICE_URI
REDIS_CLUSTER_NODES, REDIS_USERNAME, REDIS_PASSWORD
INTERNAL_HMAC_<CALLER>_<KEY_ID>_SECRET
MOBILE_ENCRYPTION_KEY_ID, MOBILE_ENCRYPTION_KEY, MOBILE_HASH_PEPPER
SMS_PROVIDER_*, CAPTCHA_*
XXL_JOB_ADMIN_ADDRESSES, XXL_JOB_ACCESS_TOKEN
```

真实密码、Pepper、HMAC、短信密钥、恢复码和管理员密码不得进入仓库。后续接入 Nacos 时只迁移地址发现和非秘密配置；秘密仍优先使用 Secret/密钥管理服务。

Gateway 目标路由：

```text
/api/auth/v1/** -> AUTH_SERVICE_URI
/api/iam/v1/**  -> IAM_SERVICE_URI
```

内部路径显式拒绝外部路由。开发期使用静态 URI；接入 Nacos 后保持路径和 DTO 不变。

## 18. 仍需在对应实施阶段确认的外部技术项

以下不是业务规则缺口，而是依赖环境或供应商的信息；确认前相关代码保留 `TODO(待确认)`，不能伪造：

| 项目 | 最晚确认阶段 | 建议/要求 |
| --- | --- | --- |
| IAM/Auth 数据库和生产账号 | 本地已建两个独立库，生产仍待确认 | 本地可使用同一账号；真实名称和凭据只进入 `application-local.yml`/安全配置，不写入设计文档 |
| 数据库变更管理 | 已确认 | 当前使用根目录 `sql` 的编号脚本、CHANGELOG 和人工执行记录，暂不引入 Flyway；未来接入工具需制定基线接管方案 |
| 密码哈希算法和成本 | 已确认并实现 v1 | Argon2id；16 字节盐、32 字节摘要、并行度 1、64 MiB、3 次迭代；上线前继续做目标机器压测 |
| 图形验证码实现 | 登录防护前 | 需无障碍/替代策略和服务端票据 |
| 短信供应商、模板和回执 | 短信编码前 | 统一 `SmsClient`，不得散落 SDK 调用 |
| IAM 服务端口和真实静态地址 | 首次联调前 | 进入 local 环境，不提交生产值 |
| XXL-JOB 版本、执行器和告警 | Outbox 投递前 | 与现有基础设施版本统一 |
| 默认机构类型模板最终编码 | 租户初始化迁移前 | 由业务确认，发布后按版本复制 |
| 各业务模块及归属字段 | 模块接入前 | 逐模块评审，禁止默认猜 `created_by` |
| 短信小时/每日次数和平台安全上限 | 安全测试前 | 配置化并经风控确认 |

HMAC v1、接口路径、表结构和权限码在本文中是建议冻结的开发基线；首次实现 PR 应进行一次契约评审，评审通过后禁止随意改变已发布语义。

## 19. 数据库迁移顺序

1. `sql/iam/ddl`：sequence、平台/租户、全局用户名注册表。
2. `sql/iam/ddl`：机构类型、关系、机构、closure、编码注册表和所有者。
3. `sql/iam/ddl`：账号、工号注册、角色、用户角色。
4. `sql/iam/ddl`：权限资源、租户功能、菜单和数据范围。
5. `sql/iam/ddl` 与 `sql/iam/dml`：审计、Outbox、幂等记录及必要初始化数据。
6. `sql/auth/ddl`：凭证、密码历史、手机号和挑战票据。
7. `sql/auth/ddl`：设备会话、权限快照、消费幂等和审计。
8. 先部署能兼容旧路由的 Auth/IAM，再更新 Gateway 路由，最后发布前端。

脚本按根目录 `sql/README.md` 编号并登记到 `sql/CHANGELOG.md`。每个已在共享环境执行的脚本不可修改，只能新增更高编号修正，并保留非敏感执行记录。数据库集成测试使用真实 PostgreSQL 或 Testcontainers PostgreSQL，不能使用 H2 证明序列、部分索引、锁和 `SKIP LOCKED` 行为。

## 20. 实施阶段和可交付物

### 阶段 0：契约冻结

- 评审本文、确认第 18 节外部项中本阶段必需部分。
- 建立 OpenAPI v1，并在根目录 `sql` 登记首批数据库脚本。
- 把 `ApiResponse`、`PageResult`、错误码、ID 字符串规则生成前端契约类型。
- 验收：OpenAPI 无未说明字段，权限码和表字段有注释，契约测试可执行。

### 阶段 1：IAM 基础域

- 新建 `smile-iam-service`、独立配置和数据库。
- 完成租户、机构类型、机构树、账号、角色、权限资源基础模型。
- 完成平台租户创建和根机构所有者初始化编排。
- 验收：跨租户数据不可见；机构环/非法类型/永久编码复用均被拒绝。

当前实现状态：平台端已具备机构类型模板整版发布、五步创建租户、IAM 本地事务初始化、IAM → Auth HMAC 凭证容器同步、幂等重试状态和实时租户列表。租户所有者 24 小时一次性激活链接、租户用户名密码登录、平台管理员密码再认证，以及无手机号场景下 30 分钟一次性密码恢复链接已经形成可运行纵切面。租户独立机构类型、允许关系 DAG、按需分页机构树、机构新增/修改/迁移/停用/恢复和空白机构删除也已形成真实接口闭环；用户、角色和数据权限 CRUD 继续按阶段 3 实施。

### 阶段 2：Auth 登录和会话

- 密码凭证、短信挑战、手机号多账号选择、Sa-Token 独立设备会话。
- Auth → IAM 登录解析 Client、HMAC、防重放。
- 完成失败计数、图形验证码门槛、锁定和记住我策略。
- 验收：多账号选择票据不能篡改/重放；不同前端入口 Token 不串用。

当前短信基础设施状态：Auth 已建立可替换的 `SmsClient` 策略边界和统一发送编排，本地环境使用 `LOCAL_DATABASE_LOG` 策略，不连接或伪造真实运营商。所有发送请求先通过数据库唯一请求 ID 原子防重，再保存手机号 HMAC 摘要、脱敏号码、短信业务用途、投递状态以及可直接阅读的正文快照；验证码、Token 和一次性秘密在进入数据库或普通日志前统一替换为 `******`。平台端已增加短信发送记录页面，完整手机号只用于 Auth 内存中的 HMAC 精确查询，列表仅展示脱敏号码并限制最长 90 天时间范围。`AUTH_DDL_0007` 与 `IAM_DML_0004` 已由用户确认在 LOCAL 环境执行，本地短信策略和手机号摘要 Pepper 也已配置。验证码挑战创建、五分钟有效期、六十秒重发限制、手机号/IP/设备小时与每日限流、错误五次失效及成功原子消费已经实现；本地策略不初始化真实供应商和模板数据所需的空配置约束调整由 `AUTH_DDL_0008` 提供。当前登录租户账号首次绑定手机号已经形成纵切面：绑定挑战受登录保护，确认接口原子消费验证码并核对手机号摘要，完整手机号使用可轮换 AES-256-GCM 密钥加密入库，页面与审计只展示脱敏号码；同一手机号仍可绑定不同账号。手机号验证码登录与多账号选择也已打通：Auth 按手机号摘要批量取得绑定账号 ID，IAM 通过 HMAC 内部接口批量复核账号、租户、机构、有效期和会话策略；单账号直接登录，多账号使用五分钟一次性数据库票据、候选集合摘要和 Auth 最近登录记录完成选择，票据不可篡改或重放。真实短信供应商仍待后续依据 SDK 文档新增策略实现，手机号自助找回密码将在下一纵切面复用已验证绑定。

当前设备会话纵切面已经补齐：租户管理端与租户业务端共用 Auth 的真实分页契约，可查看脱敏登录 IP、登录方式、设备名称、最近活动和双重失效时间；账号可按乐观锁下线指定设备，或保留当前设备并下线其它全部设备。数据库状态更新在本地 Service 事务内完成，Redis/Sa-Token 下线在事务外执行，避免远程状态操作扩大数据库事务；会话页不输出明文 Token 或 Token 摘要。

### 阶段 3：租户管理端

- 用户、角色、权限、数据范围、机构树和所有者页面。
- 激活、绑定、管理员重置、敏感操作再认证。
- 验收：普通管理员不能修改所有者或管理员同级；授权不能超过自身上限。

当前实现状态：租户管理端已经具备所有者激活、用户名密码登录和一次性密码恢复页面。密码恢复要求平台管理员填写原因并使用当前密码再认证，签发后账号全部设备立即下线、旧密码停止登录、旧恢复链接失效；新密码受默认策略和最近五次历史约束，成功后恢复 IAM/Auth 双侧状态并记录 IAM 审计。登录后的租户管理布局读取 IAM 最终权限上下文，机构类型与允许关系页面、按需分页机构树页面均已连接真实后端接口，写操作统一使用防重复提交和统一反馈出口。平台端已经增加权限资源草稿、发布和永久废弃入口；发布时自动为既有租户补齐功能开关或菜单配置。租户端已经增加本机构角色 CRUD、授权上限、功能权限整版替换、默认数据范围与模块覆盖整版替换；所有者角色受保护。租户用户管理已经覆盖真实分页、创建待激活账号、资料与有效期修改、角色整版分配、停用、恢复、不可恢复注销和一次性激活链接；创建账号与角色分配要求“原因 + 当前密码加密再认证”，票据绑定主体与唯一动作且原子消费。角色、有效期和生命周期变化会提升账号授权版本并写入本地 Outbox，等待阶段 4 由 Auth 幂等消费并完成全设备强制下线。

租户安全策略已经形成根机构所有者专用闭环：租户管理端可维护并发登录、最大设备数、记住我、普通与记住我会话双重时限、密码定期到期及审计保留时间。修改必须填写原因并消费绑定 `TENANT_SECURITY_POLICY_UPDATE` 的密码再认证票据；策略收紧时 IAM 在同一事务提升租户安全版本并按有效账号写入 Outbox，Auth 继续复用既有幂等消费链路使租户账号全部设备重新登录。菜单与按钮权限由待人工执行的 `IAM_DML_0006` 正式发布，未执行前前端不会伪造权限或强行展示入口。

### 阶段 4：授权失效和审计

当前实现状态：IAM 已使用 PostgreSQL `FOR UPDATE SKIP LOCKED` 有界领取 Outbox，支持处理中租约恢复、指数退避加抖动、最大重试次数和 `DEAD` 状态；投递由 XXL-JOB 处理器 `iamOutboxDeliveryJob` 触发，并通过既有 HMAC RestClient 调用 Auth。角色授权变化已改为在数据库内按受影响账号生成事件，避免 Auth 越界读取 IAM 数据。Auth 已实现 `POST /internal/auth/v1/security-events`，同时校验签名事件 ID、事件信封和账号/租户冗余字段，以 `eventId + payloadDigest` 幂等消费，并失效权限快照、数据库设备会话及 Sa-Token 全部设备会话。IAM 与 Auth 审计分页已经按数据所有权拆分：前端只调用 IAM，IAM 自有管理审计直接查询 PostgreSQL，认证审计经内部网络、HMAC 和静态 Auth Client 查询；平台、租户所有者和普通管理员分别应用独立 SQL 数据边界。平台端和租户管理端均已增加中文审计页面，支持来源、结果、动作编码、操作者和最长 90 天时间范围筛选；设备会话与登录成功审计在 Auth 同一事务写入。`IAM_DML_0003` 已由用户确认在 LOCAL 环境执行，平台审计与租户审计菜单权限已经发布；XXL-JOB 管理端仍需维持秒级或业务可接受频率的调度任务。

审计可读性已经进一步收口：列表以中文动作、身份类型、目标类型和失败原因作为主展示，原始编码、对象 ID 与链路 ID 仅在详情中提供给排障人员；新产生的 IAM/Auth 审计同时固化操作人用户名、显示名称、目标对象名称和动作名称快照，避免账号、角色、机构或展示名称变化后历史语义漂移。动作名称由业务事件产生处写入，IAM 权限型动作优先使用权限资源名称，Auth 登录和会话安全事件使用各自领域定义；查询层不再维护持续增长的动作编码 `case`。旧审计不回填或猜测动作名称，空快照统一展示“未登记操作”。`IAM_DDL_0008`、`IAM_DDL_0009`、`AUTH_DDL_0005` 与 `AUTH_DDL_0006` 已由用户确认在 LOCAL 环境执行。审计详情采用不遮挡列表的并排侧栏，整行点击可直接切换当前详情。

- 授权版本、权限快照、Outbox、Auth 幂等消费、全设备下线。
- IAM/Auth 审计查询、脱敏和保留策略。
- 验收：角色任意增删权限后旧 Token 立即不可继续访问；事件重试不重复下线或污染状态。

### 阶段 5：平台端和生产加固

- 平台风险二次验证、恢复码专用恢复流程、租户生命周期、模板和权限发布。
- 限流、密钥轮换、故障演练、容量和慢查询治理。
- 验收：租户停用即全员下线且无法重登；恢复码一次使用；平台/租户身份严格隔离。

## 21. 测试矩阵

### 21.1 单元与契约测试

- 用户名 Unicode/大小写归一化和永久占用。
- 密码策略、历史 5 次、失败 3/5 次边界。
- 机构类型 DAG、closure 新增/迁移/停用继承。
- 多角色功能和数据范围并集、授权上限比较。
- HMAC 规范串、过期、错误 Key、Nonce 重放。
- OpenAPI 与前端 `contracts` 类型一致性。

### 21.2 PostgreSQL/Redis 集成测试

- sequence bigint、唯一/部分索引、乐观锁冲突。
- 并发创建相同用户名、工号、机构编码只有一个成功。
- 并发转让所有者始终仅一个有效所有者。
- `FOR UPDATE SKIP LOCKED` 多实例 Outbox 投递不重复。
- Redis Cluster 下所有 Key 使用 DB 0、具备 TTL、无 Key 扫描。

### 21.3 端到端场景

1. 手机绑定三个账号，验证码后选择最近账号，选择票据第二次使用失败。
2. 租户关闭并发登录，新设备登录后旧设备立即失效。
3. 角色删除权限，用户所有设备失效，重新登录后按钮和接口都不可用。
4. 父机构停用，子机构账号不可登录；恢复父机构后单独停用子机构仍不可登录。
5. 普通管理员尝试授予超出自身的租户全部范围，被服务端拒绝并记录审计。
6. 无手机号用户通过 30 分钟重置链接改密，链接不具有业务登录能力且只能使用一次。
7. 租户注销后即使 Outbox 暂时投递失败，登录实时状态校验仍拒绝访问。
8. 平台 Token 调租户入口、租户 Token 调平台入口均返回 403/入口不匹配。

### 21.4 非功能验收

- 所有列表分页且 `pageSize <= 100`，机构树按需加载。
- SQL 无 `SELECT *`、N+1、循环查库、全量 Java 过滤。
- 登录、授权解析和常用列表记录 P95/P99、慢 SQL、Redis 命中和连接池指标。
- 日志扫描不出现密码、验证码、Token、完整手机号、链接和密钥。
- Auth/IAM 任一短暂不可用时行为符合“有效快照可用、无法验证则失败关闭”。

## 22. 编码完成定义（Definition of Done）

每个实施任务必须同时满足：

- 已阅读现有目录、POM、统一响应/异常、Redis 和同类实现；
- Controller、Service、Mapper 分层清晰，Entity 不进入接口；
- DTO 使用 Spring Validation，类、方法、枚举、字段和变量有说明性注释；
- 权限在后端最终校验，数据范围进入 SQL；
- 敏感写操作具备原因、再认证、幂等、乐观锁、审计和会话失效；
- 前端只使用统一 Axios、统一反馈和双层重复请求保护；
- 新接口已进入 OpenAPI/契约类型，不存在 `any`、假字段或假权限码；
- PostgreSQL/Redis 集成测试、单元测试、前端 typecheck/build 和 Maven test 均通过；
- 配置无真实秘密，已验证 local/dev/test/prod 的差异；
- 迁移、回滚/补偿、监控告警和联调记录齐全。

## 23. 首个开发迭代建议

第一个迭代只做“可登录的最小纵切面”，不同时铺开所有页面：

1. 冻结通用响应、分页、错误码、HMAC v1 和登录 DTO。
2. 创建 `smile-iam-service` 及租户、机构、账号、用户名注册、角色最小表。
3. 创建一个本地开发租户、根机构、所有者角色和待激活账号的正式迁移/初始化流程，不提交真实密码。
4. Auth 实现账号激活、用户名密码登录、IAM 登录快照 Client 和单设备会话。
5. Gateway 拆分 Auth/IAM 路由。
6. `tenant-admin` 实现登录、启动信息和只读用户/机构页面。
7. 用 PostgreSQL + Redis Cluster 完成端到端测试后，再进入短信多账号、角色编辑和数据权限。

这样每个阶段都能形成真实契约和可验证闭环，同时保持 Auth/IAM 数据边界，不会退化为单体服务或依赖前端伪造数据。
