# Web 多应用工作区

`web` 使用 pnpm workspace 管理三个独立的 Vue 3 + Vite + TypeScript 应用。三个应用可以独立启动、构建和部署，禁止混入 Vue 2 写法或依赖。

## 应用边界

```text
apps/
├─ platform-admin/  # 平台超级管理员入口，与租户身份隔离
├─ tenant-admin/    # 租户根所有者、机构所有者和授权管理员入口
└─ tenant-portal/   # 租户普通业务用户入口
```

每个应用内部继续遵循：

```text
src/
├─ app/                 # 路由、Pinia 和应用装配
├─ assets/styles/       # 应用级全局样式
├─ modules/<module>/    # 业务领域分包
└─ shared/              # 当前应用的统一出口和适配
```

## 共享包

```text
packages/
├─ contracts/   # 已被后端确认的跨应用契约类型
├─ http-core/   # Axios 创建器、统一响应处理和重复请求保护
├─ shared/      # 统一反馈与 useSingleFlight
└─ ui/          # 无业务归属的共享 UI
```

共享包不得承载平台、租户管理或租户业务逻辑。没有真实后端契约时，不得在 `contracts` 中猜测接口字段。

## HTTP 与反馈规则

每个应用必须通过自己的 `src/shared/http/client.ts` 创建并导出唯一 Axios 实例。三个应用复用同一 HTTP 核心，但使用不同 Token 存储空间，禁止共享 Axios 实例或 Token。

业务 API 只能导入当前应用 `@/shared/http` 暴露的 `http`。成功、失败和警告只能经过 `@/shared/feedback`。写操作使用 `useSingleFlight` 控制按钮提交态，Axios 层同时拒绝相同并发请求。

## 安装与验证

```bash
pnpm install
pnpm typecheck
pnpm build
```

## 独立启动

```bash
pnpm dev:platform
pnpm dev:tenant-admin
pnpm dev:tenant-portal
```

默认端口：

- 平台管理端：`5173`
- 租户管理端：`5174`
- 租户业务端：`5175`

也可以同时启动三个应用：

```bash
pnpm dev:all
```

三个应用的开发代理默认指向 `http://localhost:8080`，可以分别通过应用环境变量 `VITE_API_PROXY_TARGET` 修改。真实 API 基础地址可以通过 `VITE_API_BASE_URL` 提供。
