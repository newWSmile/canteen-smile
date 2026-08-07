-- script_id: IAM_DDL_0006
-- target_database: IAM
-- type: DDL
-- depends_on: IAM_DDL_0005
-- transactional: YES
-- purpose: 补齐现有 IAM 表全部字段的中文用途说明
-- rollback: 注释属于元数据说明，不执行回滚；后续修正只能新增更高编号脚本

SET search_path TO public;

BEGIN;

-- 已执行的历史脚本永久冻结；本脚本只补充字段注释，不改变表结构、约束、索引或业务数据。

-- iam_platform_identity
COMMENT ON COLUMN public.iam_platform_identity.id IS '本表记录的主键 ID';

-- iam_tenant
COMMENT ON COLUMN public.iam_tenant.id IS '本表记录的主键 ID';

-- iam_tenant_code_registry
COMMENT ON COLUMN public.iam_tenant_code_registry.id IS '本表记录的主键 ID';

-- iam_tenant_security_policy
COMMENT ON COLUMN public.iam_tenant_security_policy.id IS '本表记录的主键 ID';

-- iam_username_registry
COMMENT ON COLUMN public.iam_username_registry.id IS '本表记录的主键 ID';

-- iam_org_type_template
COMMENT ON COLUMN public.iam_org_type_template.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_org_type_template.name IS '当前记录的业务名称';
COMMENT ON COLUMN public.iam_org_type_template.sort_order IS '同级记录的显示排序值';
COMMENT ON COLUMN public.iam_org_type_template.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_org_type_template.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_org_type_template.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_org_type_template.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_org_type_template.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_org_type_template.version IS '并发更新控制使用的乐观锁版本';

-- iam_org_type_template_relation
COMMENT ON COLUMN public.iam_org_type_template_relation.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_org_type_template_relation.template_version IS '机构类型模板版本号';
COMMENT ON COLUMN public.iam_org_type_template_relation.parent_type_code IS '允许作为父节点的机构类型编码';
COMMENT ON COLUMN public.iam_org_type_template_relation.child_type_code IS '允许作为子节点的机构类型编码';
COMMENT ON COLUMN public.iam_org_type_template_relation.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_org_type_template_relation.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_org_type_template_relation.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_org_type_template_relation.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_org_type_template_relation.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_org_type_template_relation.version IS '并发更新控制使用的乐观锁版本';

-- iam_admin_region
COMMENT ON COLUMN public.iam_admin_region.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_admin_region.name IS '当前记录的业务名称';
COMMENT ON COLUMN public.iam_admin_region.level_code IS '行政区域层级编码';
COMMENT ON COLUMN public.iam_admin_region.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.iam_admin_region.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_admin_region.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_admin_region.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_admin_region.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_admin_region.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_admin_region.version IS '并发更新控制使用的乐观锁版本';

-- iam_org_type
COMMENT ON COLUMN public.iam_org_type.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_org_type.name IS '当前记录的业务名称';
COMMENT ON COLUMN public.iam_org_type.sort_order IS '同级记录的显示排序值';
COMMENT ON COLUMN public.iam_org_type.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.iam_org_type.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_org_type.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_org_type.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_org_type.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_org_type.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_org_type.version IS '并发更新控制使用的乐观锁版本';

-- iam_org_type_relation
COMMENT ON COLUMN public.iam_org_type_relation.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_org_type_relation.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_org_type_relation.parent_type_id IS '允许作为父节点的租户机构类型 ID';
COMMENT ON COLUMN public.iam_org_type_relation.child_type_id IS '允许作为子节点的租户机构类型 ID';
COMMENT ON COLUMN public.iam_org_type_relation.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_org_type_relation.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_org_type_relation.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_org_type_relation.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_org_type_relation.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_org_type_relation.version IS '并发更新控制使用的乐观锁版本';

-- iam_organization
COMMENT ON COLUMN public.iam_organization.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_organization.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_organization.name IS '当前记录的业务名称';
COMMENT ON COLUMN public.iam_organization.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_organization.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_organization.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_organization.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_organization.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_organization.version IS '并发更新控制使用的乐观锁版本';

-- iam_organization_closure
COMMENT ON COLUMN public.iam_organization_closure.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_organization_closure.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_organization_closure.ancestor_id IS '闭包关系中的祖先机构 ID';
COMMENT ON COLUMN public.iam_organization_closure.descendant_id IS '闭包关系中的后代机构 ID';
COMMENT ON COLUMN public.iam_organization_closure.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_organization_closure.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_organization_closure.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_organization_closure.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_organization_closure.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_organization_closure.version IS '并发更新控制使用的乐观锁版本';

-- iam_org_code_registry
COMMENT ON COLUMN public.iam_org_code_registry.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_org_code_registry.reserved_time IS '业务标识首次被永久占用的时间';
COMMENT ON COLUMN public.iam_org_code_registry.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_org_code_registry.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_org_code_registry.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_org_code_registry.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_org_code_registry.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_org_code_registry.version IS '并发更新控制使用的乐观锁版本';

-- iam_org_name_history
COMMENT ON COLUMN public.iam_org_name_history.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_org_name_history.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_org_name_history.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.iam_org_name_history.old_name IS '机构变更前的名称';
COMMENT ON COLUMN public.iam_org_name_history.new_name IS '机构变更后的名称';
COMMENT ON COLUMN public.iam_org_name_history.changed_by IS '执行本次变更的身份 ID';
COMMENT ON COLUMN public.iam_org_name_history.changed_time IS '业务变更发生时间';
COMMENT ON COLUMN public.iam_org_name_history.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_org_name_history.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_org_name_history.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_org_name_history.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_org_name_history.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_org_name_history.version IS '并发更新控制使用的乐观锁版本';

-- iam_account
COMMENT ON COLUMN public.iam_account.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_account.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_account.effective_at IS '账号开始允许登录和使用的时间';
COMMENT ON COLUMN public.iam_account.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.iam_account.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_account.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_account.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_account.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_account.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_account.version IS '并发更新控制使用的乐观锁版本';

-- iam_employee_number_registry
COMMENT ON COLUMN public.iam_employee_number_registry.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_employee_number_registry.normalized_employee_number IS '按服务端统一规则归一化后的工号';
COMMENT ON COLUMN public.iam_employee_number_registry.original_employee_number IS '用户录入并用于展示审计的原始工号';
COMMENT ON COLUMN public.iam_employee_number_registry.reserved_time IS '业务标识首次被永久占用的时间';
COMMENT ON COLUMN public.iam_employee_number_registry.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_employee_number_registry.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_employee_number_registry.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_employee_number_registry.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_employee_number_registry.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_employee_number_registry.version IS '并发更新控制使用的乐观锁版本';

-- iam_role
COMMENT ON COLUMN public.iam_role.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_role.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_role.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.iam_role.name IS '当前记录的业务名称';
COMMENT ON COLUMN public.iam_role.description IS '当前记录的用途和业务说明';
COMMENT ON COLUMN public.iam_role.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_role.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_role.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_role.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_role.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_role.version IS '并发更新控制使用的乐观锁版本';

-- iam_account_role
COMMENT ON COLUMN public.iam_account_role.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_account_role.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_account_role.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.iam_account_role.account_id IS '关联的租户账号 ID';
COMMENT ON COLUMN public.iam_account_role.role_id IS '关联的机构角色 ID';
COMMENT ON COLUMN public.iam_account_role.assigned_by IS '执行角色分配的账号 ID';
COMMENT ON COLUMN public.iam_account_role.assigned_time IS '角色分配完成时间';
COMMENT ON COLUMN public.iam_account_role.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_account_role.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_account_role.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_account_role.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_account_role.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_account_role.version IS '并发更新控制使用的乐观锁版本';

-- iam_org_owner
COMMENT ON COLUMN public.iam_org_owner.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_org_owner.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_org_owner.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.iam_org_owner.account_id IS '关联的租户账号 ID';
COMMENT ON COLUMN public.iam_org_owner.effective_time IS '当前所有者关系开始生效的时间';
COMMENT ON COLUMN public.iam_org_owner.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_org_owner.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_org_owner.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_org_owner.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_org_owner.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_org_owner.version IS '并发更新控制使用的乐观锁版本';

-- iam_org_owner_history
COMMENT ON COLUMN public.iam_org_owner_history.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_org_owner_history.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_org_owner_history.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.iam_org_owner_history.changed_time IS '业务变更发生时间';
COMMENT ON COLUMN public.iam_org_owner_history.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_org_owner_history.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_org_owner_history.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_org_owner_history.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_org_owner_history.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_org_owner_history.version IS '并发更新控制使用的乐观锁版本';

-- iam_permission_resource
COMMENT ON COLUMN public.iam_permission_resource.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_permission_resource.resource_type IS '权限资源类型，区分菜单、按钮和接口等资源';
COMMENT ON COLUMN public.iam_permission_resource.parent_id IS '当前节点的直接上级记录 ID';
COMMENT ON COLUMN public.iam_permission_resource.name IS '当前记录的业务名称';
COMMENT ON COLUMN public.iam_permission_resource.description IS '当前记录的用途和业务说明';
COMMENT ON COLUMN public.iam_permission_resource.app_code IS '配置适用的前端应用编码';
COMMENT ON COLUMN public.iam_permission_resource.route_path IS '菜单资源对应的前端路由路径';
COMMENT ON COLUMN public.iam_permission_resource.api_method IS '受保护接口的 HTTP 请求方法';
COMMENT ON COLUMN public.iam_permission_resource.api_path_pattern IS '受保护接口的路径匹配模式';
COMMENT ON COLUMN public.iam_permission_resource.feature_code IS '租户功能开关对应的平台功能编码';
COMMENT ON COLUMN public.iam_permission_resource.semantic_version IS '权限标识当前语义版本';
COMMENT ON COLUMN public.iam_permission_resource.sort_order IS '同级记录的显示排序值';
COMMENT ON COLUMN public.iam_permission_resource.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_permission_resource.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_permission_resource.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_permission_resource.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_permission_resource.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_permission_resource.version IS '并发更新控制使用的乐观锁版本';

-- iam_permission_api_binding
COMMENT ON COLUMN public.iam_permission_api_binding.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_permission_api_binding.permission_id IS '关联的平台权限资源 ID';
COMMENT ON COLUMN public.iam_permission_api_binding.api_resource_id IS '关联的接口权限资源 ID';
COMMENT ON COLUMN public.iam_permission_api_binding.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_permission_api_binding.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_permission_api_binding.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_permission_api_binding.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_permission_api_binding.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_permission_api_binding.version IS '并发更新控制使用的乐观锁版本';

-- iam_role_permission
COMMENT ON COLUMN public.iam_role_permission.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_role_permission.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_role_permission.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.iam_role_permission.role_id IS '关联的机构角色 ID';
COMMENT ON COLUMN public.iam_role_permission.permission_id IS '关联的平台权限资源 ID';
COMMENT ON COLUMN public.iam_role_permission.granted_by IS '执行授权的身份 ID';
COMMENT ON COLUMN public.iam_role_permission.granted_time IS '授权开始生效的时间';
COMMENT ON COLUMN public.iam_role_permission.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_role_permission.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_role_permission.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_role_permission.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_role_permission.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_role_permission.version IS '并发更新控制使用的乐观锁版本';

-- iam_tenant_feature
COMMENT ON COLUMN public.iam_tenant_feature.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_tenant_feature.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_tenant_feature.feature_code IS '租户功能开关对应的平台功能编码';
COMMENT ON COLUMN public.iam_tenant_feature.enabled IS '配置或授权是否启用';
COMMENT ON COLUMN public.iam_tenant_feature.changed_by IS '执行本次变更的身份 ID';
COMMENT ON COLUMN public.iam_tenant_feature.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_tenant_feature.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_tenant_feature.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_tenant_feature.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_tenant_feature.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_tenant_feature.version IS '并发更新控制使用的乐观锁版本';

-- iam_tenant_menu_config
COMMENT ON COLUMN public.iam_tenant_menu_config.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_tenant_menu_config.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_tenant_menu_config.menu_permission_id IS '关联的菜单权限资源 ID';
COMMENT ON COLUMN public.iam_tenant_menu_config.hidden IS '用户是否主动隐藏该菜单';
COMMENT ON COLUMN public.iam_tenant_menu_config.changed_by IS '执行本次变更的身份 ID';
COMMENT ON COLUMN public.iam_tenant_menu_config.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_tenant_menu_config.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_tenant_menu_config.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_tenant_menu_config.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_tenant_menu_config.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_tenant_menu_config.version IS '并发更新控制使用的乐观锁版本';

-- iam_account_menu_preference
COMMENT ON COLUMN public.iam_account_menu_preference.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_account_menu_preference.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_account_menu_preference.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.iam_account_menu_preference.account_id IS '关联的租户账号 ID';
COMMENT ON COLUMN public.iam_account_menu_preference.menu_permission_id IS '关联的菜单权限资源 ID';
COMMENT ON COLUMN public.iam_account_menu_preference.hidden IS '用户是否主动隐藏该菜单';
COMMENT ON COLUMN public.iam_account_menu_preference.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_account_menu_preference.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_account_menu_preference.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_account_menu_preference.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_account_menu_preference.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_account_menu_preference.version IS '并发更新控制使用的乐观锁版本';

-- iam_data_module
COMMENT ON COLUMN public.iam_data_module.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_data_module.module_code IS '数据权限所属业务模块编码';
COMMENT ON COLUMN public.iam_data_module.service_code IS '资源或数据模块所属微服务编码';
COMMENT ON COLUMN public.iam_data_module.name IS '当前记录的业务名称';
COMMENT ON COLUMN public.iam_data_module.publish_status IS '平台资源的发布状态';
COMMENT ON COLUMN public.iam_data_module.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_data_module.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_data_module.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_data_module.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_data_module.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_data_module.version IS '并发更新控制使用的乐观锁版本';

-- iam_role_data_policy
COMMENT ON COLUMN public.iam_role_data_policy.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_role_data_policy.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_role_data_policy.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.iam_role_data_policy.role_id IS '关联的机构角色 ID';
COMMENT ON COLUMN public.iam_role_data_policy.scope_type IS '角色数据权限范围类型';
COMMENT ON COLUMN public.iam_role_data_policy.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_role_data_policy.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_role_data_policy.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_role_data_policy.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_role_data_policy.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_role_data_policy.version IS '并发更新控制使用的乐观锁版本';

-- iam_role_data_scope_org
COMMENT ON COLUMN public.iam_role_data_scope_org.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_role_data_scope_org.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_role_data_scope_org.policy_id IS '关联的角色数据权限策略 ID';
COMMENT ON COLUMN public.iam_role_data_scope_org.organization_id IS '记录所属或关联的机构 ID';
COMMENT ON COLUMN public.iam_role_data_scope_org.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_role_data_scope_org.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_role_data_scope_org.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_role_data_scope_org.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_role_data_scope_org.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_role_data_scope_org.version IS '并发更新控制使用的乐观锁版本';

-- iam_audit_log
COMMENT ON COLUMN public.iam_audit_log.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_audit_log.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_audit_log.operator_type IS '执行操作的身份类型';
COMMENT ON COLUMN public.iam_audit_log.operator_organization_id IS '操作人执行操作时所属的机构 ID';
COMMENT ON COLUMN public.iam_audit_log.action_code IS '审计或授权操作的动作编码';
COMMENT ON COLUMN public.iam_audit_log.target_type IS '被操作或审计目标的业务类型';
COMMENT ON COLUMN public.iam_audit_log.target_id IS '被操作或审计目标的业务 ID';
COMMENT ON COLUMN public.iam_audit_log.reason IS '敏感操作或变更的必填原因';
COMMENT ON COLUMN public.iam_audit_log.result IS '审计事件或安全操作的执行结果';
COMMENT ON COLUMN public.iam_audit_log.ip_hash IS '访问来源 IP 的不可逆摘要';
COMMENT ON COLUMN public.iam_audit_log.device_summary IS '经过脱敏处理的设备信息摘要';
COMMENT ON COLUMN public.iam_audit_log.trace_id IS '关联请求的分布式链路追踪 ID';
COMMENT ON COLUMN public.iam_audit_log.occurred_time IS '安全或审计事件实际发生时间';
COMMENT ON COLUMN public.iam_audit_log.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_audit_log.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_audit_log.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_audit_log.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_audit_log.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_audit_log.version IS '并发更新控制使用的乐观锁版本';

-- iam_outbox_event
COMMENT ON COLUMN public.iam_outbox_event.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_outbox_event.event_id IS '跨服务事件的全局唯一 ID';
COMMENT ON COLUMN public.iam_outbox_event.aggregate_type IS '事件所属聚合根类型';
COMMENT ON COLUMN public.iam_outbox_event.aggregate_id IS '事件所属聚合根的业务 ID';
COMMENT ON COLUMN public.iam_outbox_event.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_outbox_event.event_type IS '跨服务事件类型编码';
COMMENT ON COLUMN public.iam_outbox_event.schema_version IS '事件载荷或契约结构版本';
COMMENT ON COLUMN public.iam_outbox_event.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.iam_outbox_event.retry_count IS '事件已经执行的投递重试次数';
COMMENT ON COLUMN public.iam_outbox_event.next_retry_time IS '事件下一次允许重试投递的时间';
COMMENT ON COLUMN public.iam_outbox_event.last_error_code IS '事件最近一次投递失败的错误码';
COMMENT ON COLUMN public.iam_outbox_event.trace_id IS '关联请求的分布式链路追踪 ID';
COMMENT ON COLUMN public.iam_outbox_event.occurred_time IS '安全或审计事件实际发生时间';
COMMENT ON COLUMN public.iam_outbox_event.published_time IS '平台资源首次发布时间';
COMMENT ON COLUMN public.iam_outbox_event.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_outbox_event.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_outbox_event.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_outbox_event.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_outbox_event.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_outbox_event.version IS '并发更新控制使用的乐观锁版本';

-- iam_idempotency_record
COMMENT ON COLUMN public.iam_idempotency_record.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_idempotency_record.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_idempotency_record.operator_type IS '执行操作的身份类型';
COMMENT ON COLUMN public.iam_idempotency_record.operator_id IS '执行操作的平台身份或租户账号 ID';
COMMENT ON COLUMN public.iam_idempotency_record.operation_code IS '幂等记录对应的业务操作编码';
COMMENT ON COLUMN public.iam_idempotency_record.idempotency_key_hash IS '客户端幂等键的不可逆摘要';
COMMENT ON COLUMN public.iam_idempotency_record.request_hash IS '幂等请求关键参数的摘要';
COMMENT ON COLUMN public.iam_idempotency_record.response_reference IS '幂等操作结果的非敏感引用';
COMMENT ON COLUMN public.iam_idempotency_record.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.iam_idempotency_record.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.iam_idempotency_record.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_idempotency_record.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_idempotency_record.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_idempotency_record.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_idempotency_record.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_idempotency_record.version IS '并发更新控制使用的乐观锁版本';

-- iam_tenant_audit_access_grant
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.tenant_id IS '记录所属的租户 ID';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.platform_identity_id IS '关联的独立平台管理身份 ID';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.reason IS '敏感操作或变更的必填原因';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.reauth_reference_hash IS '管理员再认证票据引用的不可逆摘要';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.status IS '当前记录的业务状态';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.expires_at IS '当前记录或一次性票据的失效时间';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.revoked_time IS '授权被主动撤销的时间';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.created_by IS '创建该记录的身份 ID，系统任务使用约定的系统身份';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.created_time IS '记录创建时间';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.is_deleted IS '逻辑删除标记，false 表示有效记录';
COMMENT ON COLUMN public.iam_tenant_audit_access_grant.version IS '并发更新控制使用的乐观锁版本';

COMMIT;

