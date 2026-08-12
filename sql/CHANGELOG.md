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
| `IAM_DDL_0008` | IAM | DDL | `iam/ddl/IAM_DDL_0008__add_audit_display_snapshots.sql` | `IAM_DDL_0007` | `EXECUTED` | 用户已确认在 LOCAL 环境执行；为 IAM 管理审计增加操作人用户名、显示名称和目标名称、业务编码快照 |
| `IAM_DDL_0009` | IAM | DDL | `iam/ddl/IAM_DDL_0009__add_audit_action_name_snapshot.sql` | `IAM_DDL_0008` | `EXECUTED` | 用户已确认在 LOCAL 环境执行；为 IAM 管理审计增加事件发生时的中文动作名称快照，历史记录不回填 |
| `IAM_DDL_0010` | IAM | DDL | `iam/ddl/IAM_DDL_0010__add_generic_async_audit_metadata.sql` | `IAM_DDL_0009` | `READY` | 为注解驱动异步审计增加事件幂等 ID、端和任意层级分类快照、失败码与耗时字段 |
| `IAM_DML_0001` | IAM | DML | `iam/dml/IAM_DML_0001__publish_tenant_admin_permissions.sql` | `IAM_DDL_0006` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；正式发布租户管理端 4 个菜单和 17 个已实现按钮权限，并为已有租户补齐菜单显示配置 |
| `IAM_DML_0002` | IAM | DML | `iam/dml/IAM_DML_0002__expand_pending_role_authorization_events.sql` | `IAM_DDL_0007` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；将历史角色级授权变更事件幂等展开为账号级事件，供 Auth 精确失效会话 |
| `IAM_DML_0003` | IAM | DML | `iam/dml/IAM_DML_0003__publish_audit_view_permissions.sql` | `IAM_DML_0002` | `EXECUTED` | 用户已确认在 LOCAL 环境执行，已发布平台审计与租户审计菜单权限，并为已有租户补齐审计菜单显示配置 |
| `IAM_DML_0004` | IAM | DML | `iam/dml/IAM_DML_0004__publish_sms_delivery_view_permission.sql` | `IAM_DML_0003` | `EXECUTED` | 用户已确认在 LOCAL 环境执行；发布平台短信发送记录菜单权限 |
| `IAM_DML_0005` | IAM | DML | `iam/dml/IAM_DML_0005__publish_sms_management_permissions.sql` | `IAM_DML_0004` | `EXECUTED` | 用户已确认在 LOCAL 环境执行；发布短信管理目录、短信设置、短信安全及修改权限，并将现有短信记录菜单调整为短信列表 |
| `IAM_DML_0006` | IAM | DML | `iam/dml/IAM_DML_0006__publish_tenant_security_policy_permissions.sql` | `IAM_DML_0005` | `READY` | 发布租户安全策略菜单与修改权限，并为已有租户补齐菜单显示配置 |
| `AUTH_DDL_0001` | AUTH | DDL | `auth/ddl/AUTH_DDL_0001__create_credential_config_core.sql` | 无 | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `AUTH_DDL_0002` | AUTH | DDL | `auth/ddl/AUTH_DDL_0002__create_challenge_ticket_core.sql` | `AUTH_DDL_0001` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `AUTH_DDL_0003` | AUTH | DDL | `auth/ddl/AUTH_DDL_0003__create_session_audit_core.sql` | `AUTH_DDL_0002` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行 |
| `AUTH_DDL_0004` | AUTH | DDL | `auth/ddl/AUTH_DDL_0004__complete_column_comments.sql` | `AUTH_DDL_0003` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；补齐 Auth 字段中文注释 |
| `AUTH_DDL_0005` | AUTH | DDL | `auth/ddl/AUTH_DDL_0005__add_audit_identity_snapshots.sql` | `AUTH_DDL_0004` | `EXECUTED` | 用户已确认在 LOCAL 环境执行；为 Auth 安全审计增加认证主体与操作人用户名、显示名称快照 |
| `AUTH_DDL_0006` | AUTH | DDL | `auth/ddl/AUTH_DDL_0006__add_audit_action_name_snapshot.sql` | `AUTH_DDL_0005` | `EXECUTED` | 用户已确认在 LOCAL 环境执行；为 Auth 安全审计增加事件发生时的中文动作名称快照，历史记录不回填 |
| `AUTH_DDL_0007` | AUTH | DDL | `auth/ddl/AUTH_DDL_0007__create_sms_delivery_record.sql` | `AUTH_DDL_0006` | `EXECUTED` | 用户已确认在 LOCAL 环境执行；创建短信投递记录，保留可读明文正文快照但不保存完整手机号、验证码、Token 或一次性链接，供本地日志策略和后续真实供应商策略统一使用 |
| `AUTH_DDL_0008` | AUTH | DDL | `auth/ddl/AUTH_DDL_0008__allow_local_sms_challenge_config.sql` | `AUTH_DDL_0007` | `EXECUTED` | 用户已确认在 LOCAL 环境执行；允许本地数据库日志策略创建不关联真实供应商和模板配置的验证码挑战，两个配置外键必须同时为空或同时有值 |
| `AUTH_DDL_0009` | AUTH | DDL | `auth/ddl/AUTH_DDL_0009__create_sms_runtime_policy.sql` | `AUTH_DDL_0008` | `EXECUTED` | 用户已确认在 LOCAL 环境执行；创建平台可维护的短信验证码限流和敏感正文留存策略，并标记投递记录是否保留敏感内容 |
| `AUTH_DDL_0010` | AUTH | DDL | `auth/ddl/AUTH_DDL_0010__separate_account_selector_flows.sql` | `AUTH_DDL_0009` | `READY` | 为手机号多账号选择票据增加流程用途，禁止登录票据与密码找回票据交叉复用 |
| `AUTH_DDL_0011` | AUTH | DDL | `auth/ddl/AUTH_DDL_0011__add_generic_async_audit_metadata.sql` | `AUTH_DDL_0010` | `READY` | 为注解驱动异步审计增加事件幂等 ID、端和任意层级分类快照、目标快照、原因与耗时字段 |
| `AUTH_DDL_0012` | AUTH | DDL | `auth/ddl/AUTH_DDL_0012__add_audit_operator_organization_snapshot.sql` | `AUTH_DDL_0011` | `EXECUTED` | 用户确认已在 LOCAL PostgreSQL 手工执行；为注解异步审计补充操作人所属机构 ID 快照，确保认证安全事件可以追溯真实组织归属 |

规则：一旦脚本状态进入 `EXECUTED`，文件内容、编号和文件名都不得修改；后续修正必须新增脚本并在“依赖”列关联原脚本。
