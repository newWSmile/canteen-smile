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
| `AUTH_DDL_0001` | AUTH | DDL | `auth/ddl/AUTH_DDL_0001__create_credential_config_core.sql` | 无 | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `AUTH_DDL_0002` | AUTH | DDL | `auth/ddl/AUTH_DDL_0002__create_challenge_ticket_core.sql` | `AUTH_DDL_0001` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `AUTH_DDL_0003` | AUTH | DDL | `auth/ddl/AUTH_DDL_0003__create_session_audit_core.sql` | `AUTH_DDL_0002` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `AUTH_DDL_0004` | AUTH | DDL | `auth/ddl/AUTH_DDL_0004__complete_column_comments.sql` | `AUTH_DDL_0003` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；补齐 Auth 字段中文注释 |

规则：一旦脚本状态进入 `EXECUTED`，文件内容、编号和文件名都不得修改；后续修正必须新增脚本并在“依赖”列关联原脚本。
