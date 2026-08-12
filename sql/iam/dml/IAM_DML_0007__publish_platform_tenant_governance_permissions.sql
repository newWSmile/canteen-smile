-- script_id: IAM_DML_0007
-- target_database: IAM
-- type: DML
-- depends_on: IAM_DML_0006
-- transactional: YES
-- purpose: 正式发布平台租户治理菜单及创建、修改、状态、注销和所有者激活权限
-- rollback: 权限标识发布后永久保留且禁止复用；执行后不允许删除或回滚，只能新增更高编号修正脚本

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.iam_platform_identity
        WHERE status = 'ACTIVE' AND is_deleted = false
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0007 执行失败：不存在有效的平台身份';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.iam_permission_resource
        WHERE permission_code IN (
            'platform:tenant:view', 'platform:tenant:create', 'platform:tenant:update',
            'platform:tenant:status', 'platform:tenant:cancel', 'platform:tenant-owner:activate'
        )
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0007 执行失败：平台租户治理权限码已存在，禁止覆盖、改义或复用';
    END IF;
END
$$;

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE' AND is_deleted = false
), tenant_menu AS (
    INSERT INTO public.iam_permission_resource (
        permission_code, resource_type, parent_id, name, description, app_code,
        route_path, component_key, api_method, api_path_pattern, feature_code,
        publish_status, semantic_version, sort_order, created_by, updated_by
    )
    SELECT 'platform:tenant:view', 'MENU', NULL, '租户治理',
           '查看平台管理范围内的租户及其生命周期状态',
           'PLATFORM_ADMIN', '/', NULL, NULL, NULL, NULL,
           'PUBLISHED', 1, 100, operator_identity.operator_id, operator_identity.operator_id
    FROM operator_identity
    RETURNING id, created_by
), tenant_actions (
    permission_code, name, description, api_method, api_path_pattern, sort_order
) AS (
    VALUES
        ('platform:tenant:create', '创建租户', '创建租户并同步初始化根机构、所有者、安全策略和认证凭证', 'POST', '/api/iam/v1/platform/tenants', 10),
        ('platform:tenant:update', '修改租户资料', '修改租户可变显示资料，租户业务编码保持永久不变', 'PUT', '/api/iam/v1/platform/tenants/{tenantId}', 20),
        ('platform:tenant:status', '暂停或恢复租户', '填写原因并完成平台管理员再认证后暂停或恢复租户', 'POST', '/api/iam/v1/platform/tenants/{tenantId}/actions/{action}', 30),
        ('platform:tenant:cancel', '注销租户', '填写原因并完成平台管理员再认证后不可恢复地注销租户', 'POST', '/api/iam/v1/platform/tenants/{tenantId}/actions/cancel', 40),
        ('platform:tenant-owner:activate', '生成所有者激活链接', '为待激活的租户根机构所有者签发一次性激活链接', 'POST', '/api/iam/v1/platform/tenants/{tenantId}/owner/activation-links', 50)
)
INSERT INTO public.iam_permission_resource (
    permission_code, resource_type, parent_id, name, description, app_code,
    route_path, component_key, api_method, api_path_pattern, feature_code,
    publish_status, semantic_version, sort_order, created_by, updated_by
)
SELECT tenant_actions.permission_code, 'BUTTON', tenant_menu.id,
       tenant_actions.name, tenant_actions.description, 'PLATFORM_ADMIN',
       NULL, NULL, tenant_actions.api_method, tenant_actions.api_path_pattern, NULL,
       'PUBLISHED', 1, tenant_actions.sort_order, tenant_menu.created_by, tenant_menu.created_by
FROM tenant_actions
CROSS JOIN tenant_menu;

DO $$
DECLARE
    permission_count integer;
BEGIN
    SELECT COUNT(*) INTO permission_count
    FROM public.iam_permission_resource
    WHERE permission_code IN (
        'platform:tenant:view', 'platform:tenant:create', 'platform:tenant:update',
        'platform:tenant:status', 'platform:tenant:cancel', 'platform:tenant-owner:activate'
    )
      AND publish_status = 'PUBLISHED'
      AND is_deleted = false;

    IF permission_count <> 6 THEN
        RAISE EXCEPTION 'IAM_DML_0007 校验失败：平台租户治理已发布权限数=%，预期=6', permission_count;
    END IF;
END
$$;

COMMIT;

-- 执行后核对：应返回 1 个菜单权限和 5 个按钮权限。
-- SELECT permission_code, resource_type, name, app_code, publish_status
-- FROM public.iam_permission_resource
-- WHERE permission_code LIKE 'platform:tenant%'
-- ORDER BY sort_order, id;
