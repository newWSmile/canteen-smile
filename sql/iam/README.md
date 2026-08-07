# IAM 数据库脚本

本目录只管理 `smile-iam-service` 独占数据库。IAM 拥有租户、机构、账号资料、角色、权限、数据范围、审计和 Outbox 数据，不得创建或访问 Auth 凭证表。

- `ddl/`：sequence、表、索引、约束及结构变更。
- `dml/`：机构类型模板、已确认权限资源等初始化数据，以及经过评审的数据修正。

具体表结构以根目录 `doc/iam-executable-implementation-plan.md` 为设计基线；尚未确认的字段、权限码和字典值不得写入脚本。
