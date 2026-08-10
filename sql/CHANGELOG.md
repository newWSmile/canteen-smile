# SQL 脚本目录

所有 DDL/DML 在创建时登记。状态使用 `DRAFT`、`READY`、`EXECUTED`、`SUPERSEDED`；`EXECUTED` 表示脚本已经在至少一个 PostgreSQL 环境执行并永久冻结，具体环境在说明或 `executions/` 中登记。

| 脚本 ID | 目标库 | 类型 | 文件 | 依赖 | 状态 | 说明 |
| --- | --- | --- | --- | --- | --- | --- |
| `IAM_DDL_0001` | IAM | DDL | `iam/ddl/IAM_DDL_0001__create_tenant_core.sql` | 无 | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `IAM_DDL_0002` | IAM | DDL | `iam/ddl/IAM_DDL_0002__create_organization_core.sql` | `IAM_DDL_0001` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `IAM_DDL_0003` | IAM | DDL | `iam/ddl/IAM_DDL_0003__create_account_role_core.sql` | `IAM_DDL_0001`, `IAM_DDL_0002` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `IAM_DDL_0004` | IAM | DDL | `iam/ddl/IAM_DDL_0004__create_permission_scope_core.sql` | `IAM_DDL_0003` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `IAM_DDL_0005` | IAM | DDL | `iam/ddl/IAM_DDL_0005__create_audit_outbox_core.sql` | `IAM_DDL_0004` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `IAM_DDL_0006` | IAM | DDL | `iam/ddl/IAM_DDL_0006__complete_column_comments.sql` | `IAM_DDL_0005` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；补齐 IAM 字段中文注释 |
| `IAM_DDL_0007` | IAM | DDL | `iam/ddl/IAM_DDL_0007__add_outbox_processing_lease_index.sql` | `IAM_DDL_0006` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；为处理中 Outbox 事件的超时租约恢复增加局部索引 |
| `IAM_DML_0001` | IAM | DML | `iam/dml/IAM_DML_0001__publish_tenant_admin_permissions.sql` | `IAM_DDL_0006` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；正式发布租户管理端 4 个菜单和 17 个已实现按钮权限，并为已有租户补齐菜单显示配置 |
| `IAM_DML_0002` | IAM | DML | `iam/dml/IAM_DML_0002__expand_pending_role_authorization_events.sql` | `IAM_DDL_0007` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；将历史角色级授权变更事件幂等展开为账号级事件，供 Auth 精确失效会话 |
| `IAM_DML_0003` | IAM | DML | `iam/dml/IAM_DML_0003__publish_audit_view_permissions.sql` | `IAM_DML_0002` | `READY` | 发布平台审计与租户审计菜单权限，并为已有租户补齐审计菜单显示配置 |
| `AUTH_DDL_0001` | AUTH | DDL | `auth/ddl/AUTH_DDL_0001__create_credential_config_core.sql` | 无 | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `AUTH_DDL_0002` | AUTH | DDL | `auth/ddl/AUTH_DDL_0002__create_challenge_ticket_core.sql` | `AUTH_DDL_0001` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `AUTH_DDL_0003` | AUTH | DDL | `auth/ddl/AUTH_DDL_0003__create_session_audit_core.sql` | `AUTH_DDL_0002` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `AUTH_DDL_0004` | AUTH | DDL | `auth/ddl/AUTH_DDL_0004__complete_column_comments.sql` | `AUTH_DDL_0003` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；补齐 Auth 字段中文注释 |

规则：一旦脚本状态进入 `EXECUTED`，文件内容、编号和文件名都不得修改；后续修正必须新增脚本并在“依赖”列关联原脚本。
