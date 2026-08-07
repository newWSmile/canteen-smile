# SQL 人工迭代管理

本目录集中管理 Canteen Smile 的 PostgreSQL DDL 和 DML。当前阶段**不引入 Flyway**，脚本由开发者评审后按顺序人工执行，并登记执行结果。服务模块的 `src/main/resources` 下禁止再放另一套数据库脚本。

## 目录

```text
sql/
├─ iam/
│  ├─ ddl/        # IAM 数据库建表、索引、约束和结构变更
│  └─ dml/        # IAM 初始化数据与受控数据修正
├─ auth/
│  ├─ ddl/        # Auth 数据库结构变更
│  └─ dml/        # Auth 初始化数据与受控数据修正
├─ executions/    # 人工执行记录模板和非敏感执行记录
└─ CHANGELOG.md   # 全部脚本目录及依赖顺序
```

IAM 和 Auth 是两个独立数据库，脚本不得跨库访问、创建跨库依赖或读取另一个服务的数据。

## 脚本编号

脚本 ID 和文件名必须全局明确：

```text
IAM_DDL_0001__create_tenant_core.sql
IAM_DML_0001__init_confirmed_reference_data.sql
AUTH_DDL_0001__create_credential_core.sql
AUTH_DML_0001__init_confirmed_reference_data.sql
```

- `IAM/AUTH` 表示目标数据库。
- `DDL/DML` 表示脚本类型。
- 四位数字在对应“数据库 + 类型”目录内单调递增，不得重复或回退。
- 双下划线后只使用小写英文、数字和下划线描述目的。
- 每个脚本必须登记到 `CHANGELOG.md`，并明确依赖脚本。

## 脚本头

每个 `.sql` 文件开头必须包含：

```sql
-- script_id: IAM_DDL_0001
-- target_database: IAM
-- type: DDL
-- depends_on: NONE
-- transactional: YES
-- purpose: 创建租户核心表
-- rollback: 上线前回滚方式或“仅允许新增修正脚本”
```

不得在脚本中写数据库密码、连接串、生产地址、手机号、Token、密钥或真实业务隐私数据。不得使用 `\c` 硬编码数据库名，执行时由连接目标决定数据库。

## 变更规则

1. DDL 与 DML 分开，禁止一个文件同时修改结构和业务数据。
2. 同一迭代先执行 DDL，验证通过后再执行依赖它的 DML。
3. 已在任何共享环境执行并登记的脚本永久冻结，不得修改、覆盖、改名或删除；修复必须新增更高编号脚本。
4. DDL 必须显式写字段和约束，禁止依赖客户端默认 schema 或隐式类型转换。
5. DML 的 `INSERT` 必须显式列名；`UPDATE/DELETE` 必须有可审查的 `WHERE`，并在注释中提供影响行数核对语句。
6. 能在事务中安全执行的脚本应使用 `BEGIN/COMMIT`；不能在事务内执行的 PostgreSQL 操作必须在脚本头标记 `transactional: NO` 并写明失败恢复步骤。
7. 大表结构变更、数据回填和索引创建必须评估锁表时间、批次、超时、磁盘和回滚，不得一次加载或更新全部数据。
8. 核心表继续遵守 `created_by`、`created_time`、`updated_by`、`updated_time`、`is_deleted`，主键统一 PostgreSQL sequence + bigint。
9. 每张表必须使用 `COMMENT ON TABLE` 添加明确的中文用途说明，每一个字段必须使用 `COMMENT ON COLUMN` 添加明确的中文用途说明；建表或新增字段时必须在同一迭代补齐。表注释和字段中文注释覆盖率均须达到 100%，不得仅以字段名、代码注释或文档代替数据库注释。
10. SQL 必须先在开发库和测试库验证；不得使用 H2 代替 PostgreSQL 验证。

## 人工执行

可以通过 IDEA Database、DBeaver 或 `psql` 执行。命令行示例只引用本机环境变量：

```powershell
psql -v ON_ERROR_STOP=1 -f sql/iam/ddl/IAM_DDL_0001__create_tenant_core.sql
```

连接信息由本机 PostgreSQL 客户端配置或安全环境变量提供，不把 JDBC URL 直接当作 `psql` 连接 URI。执行前核对目标数据库、当前脚本 ID、依赖脚本和备份/回滚条件；执行后记录开始时间、结束时间、操作者、环境、脚本校验值、结果和影响行数。执行记录禁止包含连接密码和敏感数据。

## 将来接入迁移工具

以后若接入 Flyway 或其它迁移工具，必须先评审现有人工执行记录、基线版本和脚本校验值，再制定一次性接管方案。不得直接把已执行脚本复制到自动迁移目录后重新执行。
