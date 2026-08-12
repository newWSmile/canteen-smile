-- script_id: IAM_DML_0006
-- target_database: IAM
-- type: DML
-- depends_on: IAM_DML_0005
-- transactional: YES
-- purpose: 发布租户安全策略菜单与修改权限，并为已有租户补齐菜单显示配置
-- rollback: 权限标识发布后永久保留且禁止复用；执行后不允许删除或回滚，只能新增更高编号修正脚本

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM public.iam_platform_identity
        WHERE status = 'ACTIVE' AND is_deleted = false
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0006 执行失败：不存在有效的平台身份';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.iam_permission_resource
        WHERE permission_code IN ('iam:tenant-security:view', 'iam:tenant-security:manage')
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0006 执行失败：租户安全策略权限码已存在，禁止覆盖、改义或复用';
    END IF;
END
$$;

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE' AND is_deleted = false
), security_menu AS (
    INSERT INTO public.iam_permission_resource (
        permission_code, resource_type, parent_id, name, description, app_code,
        route_path, component_key, api_method, api_path_pattern, feature_code,
        publish_status, semantic_version, sort_order, created_by, updated_by
    )
    SELECT 'iam:tenant-security:view', 'MENU', NULL, '租户安全策略',
           '查看租户并发设备、记住我、会话时长、密码到期和审计保留策略',
           'TENANT_ADMIN', '/tenant/security', NULL, NULL, NULL, NULL,
           'PUBLISHED', 1, 600, operator_identity.operator_id, operator_identity.operator_id
    FROM operator_identity
    RETURNING id, created_by
)
INSERT INTO public.iam_permission_resource (
    permission_code, resource_type, parent_id, name, description, app_code,
    route_path, component_key, api_method, api_path_pattern, feature_code,
    publish_status, semantic_version, sort_order, created_by, updated_by
)
SELECT 'iam:tenant-security:manage', 'BUTTON', security_menu.id, '修改租户安全策略',
       '由租户根机构所有者填写原因并完成密码再认证后修改租户安全策略',
       'TENANT_ADMIN', NULL, NULL, 'PUT', '/api/iam/v1/tenant/security-policy', NULL,
       'PUBLISHED', 1, 10, security_menu.created_by, security_menu.created_by
FROM security_menu;

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE' AND is_deleted = false
), security_menu AS (
    SELECT id
    FROM public.iam_permission_resource
    WHERE permission_code = 'iam:tenant-security:view'
      AND resource_type = 'MENU'
      AND app_code = 'TENANT_ADMIN'
      AND publish_status = 'PUBLISHED'
      AND is_deleted = false
)
INSERT INTO public.iam_tenant_menu_config (
    tenant_id, menu_permission_id, hidden, changed_by, created_by, updated_by
)
SELECT tenant.id, security_menu.id, false, operator_identity.operator_id,
       operator_identity.operator_id, operator_identity.operator_id
FROM public.iam_tenant tenant
CROSS JOIN security_menu
CROSS JOIN operator_identity
WHERE tenant.is_deleted = false
ON CONFLICT (tenant_id, menu_permission_id) DO NOTHING;

DO $$
DECLARE
    permission_count integer;
    tenant_count integer;
    config_count integer;
BEGIN
    SELECT COUNT(*) INTO permission_count
    FROM public.iam_permission_resource
    WHERE permission_code IN ('iam:tenant-security:view', 'iam:tenant-security:manage')
      AND publish_status = 'PUBLISHED' AND is_deleted = false;

    SELECT COUNT(*) INTO tenant_count
    FROM public.iam_tenant
    WHERE is_deleted = false;

    SELECT COUNT(*) INTO config_count
    FROM public.iam_tenant_menu_config config
    JOIN public.iam_permission_resource resource ON resource.id = config.menu_permission_id
    WHERE resource.permission_code = 'iam:tenant-security:view'
      AND config.is_deleted = false;

    IF permission_count <> 2 OR config_count <> tenant_count THEN
        RAISE EXCEPTION 'IAM_DML_0006 校验失败：权限数=%, 菜单配置数=%, 有效租户数=%',
            permission_count, config_count, tenant_count;
    END IF;
END
$$;

COMMIT;

-- 执行后核对：应返回一个菜单权限和一个按钮权限，每个有效租户应有菜单显示配置。
-- SELECT permission_code, resource_type, app_code, route_path, publish_status
-- FROM public.iam_permission_resource
-- WHERE permission_code IN ('iam:tenant-security:view', 'iam:tenant-security:manage');
