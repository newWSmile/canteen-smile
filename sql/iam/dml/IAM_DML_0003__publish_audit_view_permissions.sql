-- script_id: IAM_DML_0003
-- target_database: IAM
-- type: DML
-- depends_on: IAM_DML_0002
-- transactional: YES
-- purpose: 发布平台审计和租户审计菜单权限，并为已有租户补齐审计菜单显示配置
-- rollback: 权限标识发布后永久保留且禁止复用；执行后不允许删除或回滚，只能新增更高编号修正脚本

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM public.iam_platform_identity
        WHERE status = 'ACTIVE'
          AND is_deleted = false
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0003 执行失败：不存在有效的平台身份';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.iam_permission_resource
        WHERE permission_code IN ('platform:audit:view', 'iam:audit:view')
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0003 执行失败：审计权限码已存在，禁止覆盖、改义或复用';
    END IF;
END
$$;

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE'
      AND is_deleted = false
), audit_menu (
    permission_code,
    name,
    description,
    app_code,
    route_path,
    sort_order
) AS (
    VALUES
        ('platform:audit:view', '平台审计', '查看平台治理操作与平台身份认证安全审计', 'PLATFORM_ADMIN', '/audit', 400),
        ('iam:audit:view', '审计日志', '查看当前租户授权范围内的管理操作与认证安全审计', 'TENANT_ADMIN', '/audit', 500)
)
INSERT INTO public.iam_permission_resource (
    permission_code,
    resource_type,
    parent_id,
    name,
    description,
    app_code,
    route_path,
    component_key,
    api_method,
    api_path_pattern,
    feature_code,
    publish_status,
    semantic_version,
    sort_order,
    created_by,
    updated_by
)
SELECT audit_menu.permission_code,
       'MENU',
       NULL,
       audit_menu.name,
       audit_menu.description,
       audit_menu.app_code,
       audit_menu.route_path,
       NULL,
       NULL,
       NULL,
       NULL,
       'PUBLISHED',
       1,
       audit_menu.sort_order,
       operator_identity.operator_id,
       operator_identity.operator_id
FROM audit_menu
CROSS JOIN operator_identity;

-- 租户级菜单显示配置只控制导航可见性，不替代角色权限和后端最终校验。
WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE'
      AND is_deleted = false
), tenant_audit_menu AS (
    SELECT id
    FROM public.iam_permission_resource
    WHERE permission_code = 'iam:audit:view'
      AND resource_type = 'MENU'
      AND app_code = 'TENANT_ADMIN'
      AND publish_status = 'PUBLISHED'
      AND is_deleted = false
)
INSERT INTO public.iam_tenant_menu_config (
    tenant_id,
    menu_permission_id,
    hidden,
    changed_by,
    created_by,
    updated_by
)
SELECT tenant.id,
       tenant_audit_menu.id,
       false,
       operator_identity.operator_id,
       operator_identity.operator_id,
       operator_identity.operator_id
FROM public.iam_tenant tenant
CROSS JOIN tenant_audit_menu
CROSS JOIN operator_identity
WHERE tenant.is_deleted = false
ON CONFLICT (tenant_id, menu_permission_id) DO NOTHING;

DO $$
DECLARE
    published_count integer;
    tenant_config_count integer;
    active_tenant_count integer;
BEGIN
    SELECT COUNT(*) INTO published_count
    FROM public.iam_permission_resource
    WHERE permission_code IN ('platform:audit:view', 'iam:audit:view')
      AND resource_type = 'MENU'
      AND publish_status = 'PUBLISHED'
      AND semantic_version = 1
      AND parent_id IS NULL
      AND is_deleted = false;

    SELECT COUNT(*) INTO active_tenant_count
    FROM public.iam_tenant
    WHERE is_deleted = false;

    SELECT COUNT(*) INTO tenant_config_count
    FROM public.iam_tenant_menu_config menu_config
    JOIN public.iam_permission_resource permission_resource
      ON permission_resource.id = menu_config.menu_permission_id
    WHERE permission_resource.permission_code = 'iam:audit:view'
      AND menu_config.is_deleted = false;

    IF published_count <> 2 OR tenant_config_count <> active_tenant_count THEN
        RAISE EXCEPTION
            'IAM_DML_0003 校验失败：已发布审计菜单=%, 租户审计菜单配置=%, 有效租户=%',
            published_count,
            tenant_config_count,
            active_tenant_count;
    END IF;
END
$$;

COMMIT;

-- 执行后核对：应返回两个已发布菜单；每个有效租户应有一条 iam:audit:view 菜单配置。
-- SELECT permission_code, app_code, route_path, publish_status
-- FROM public.iam_permission_resource
-- WHERE permission_code IN ('platform:audit:view', 'iam:audit:view');
