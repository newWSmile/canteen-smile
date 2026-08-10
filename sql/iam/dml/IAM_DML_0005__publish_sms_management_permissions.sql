-- script_id: IAM_DML_0005
-- target_database: IAM
-- type: DML
-- depends_on: IAM_DML_0004
-- transactional: YES
-- purpose: 将短信发送记录调整到短信管理目录，并发布短信限流设置、短信安全及其修改权限
-- rollback: 权限标识发布后永久保留且禁止复用；执行后不允许删除或回滚，只能新增更高编号修正脚本

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.iam_permission_resource
        WHERE permission_code = 'platform:sms-delivery:view'
          AND publish_status = 'PUBLISHED'
          AND is_deleted = false
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0005 执行失败：短信发送记录权限尚未发布';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.iam_permission_resource
        WHERE permission_code IN (
            'platform:sms:directory',
            'platform:sms-settings:view', 'platform:sms-settings:update',
            'platform:sms-security:view', 'platform:sms-security:update'
        )
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0005 执行失败：待发布短信管理权限码已存在，禁止覆盖、改义或复用';
    END IF;
END
$$;

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE' AND is_deleted = false
), directory_resource AS (
    INSERT INTO public.iam_permission_resource (
        permission_code, resource_type, parent_id, name, description, app_code,
        route_path, publish_status, semantic_version, sort_order, created_by, updated_by
    )
    SELECT 'platform:sms:directory', 'DIRECTORY', NULL, '短信管理',
           '平台短信发送记录、验证码限流和敏感内容留存策略入口',
           'PLATFORM_ADMIN', NULL, 'PUBLISHED', 1, 450,
           operator_identity.operator_id, operator_identity.operator_id
    FROM operator_identity
    RETURNING id, created_by
), settings_menu AS (
    INSERT INTO public.iam_permission_resource (
        permission_code, resource_type, parent_id, name, description, app_code,
        route_path, publish_status, semantic_version, sort_order, created_by, updated_by
    )
    SELECT 'platform:sms-settings:view', 'MENU', directory_resource.id, '短信设置',
           '查看平台短信验证码有效期、重发间隔以及手机号、IP 和设备限流策略',
           'PLATFORM_ADMIN', '/sms/settings', 'PUBLISHED', 1, 20,
           directory_resource.created_by, directory_resource.created_by
    FROM directory_resource
    RETURNING id, created_by
), security_menu AS (
    INSERT INTO public.iam_permission_resource (
        permission_code, resource_type, parent_id, name, description, app_code,
        route_path, publish_status, semantic_version, sort_order, created_by, updated_by
    )
    SELECT 'platform:sms-security:view', 'MENU', directory_resource.id, '短信安全',
           '查看验证码明文留存安全策略及风险说明',
           'PLATFORM_ADMIN', '/sms/security', 'PUBLISHED', 1, 30,
           directory_resource.created_by, directory_resource.created_by
    FROM directory_resource
    RETURNING id, created_by
)
INSERT INTO public.iam_permission_resource (
    permission_code, resource_type, parent_id, name, description, app_code,
    publish_status, semantic_version, sort_order, created_by, updated_by
)
SELECT 'platform:sms-settings:update', 'BUTTON', settings_menu.id, '修改短信限流设置',
       '修改验证码有效期、重发间隔和多维发送限流', 'PLATFORM_ADMIN',
       'PUBLISHED', 1, 10, settings_menu.created_by, settings_menu.created_by
FROM settings_menu
UNION ALL
SELECT 'platform:sms-security:update', 'BUTTON', security_menu.id, '修改短信安全设置',
       '开启或关闭后续验证码短信正文的明文留存', 'PLATFORM_ADMIN',
       'PUBLISHED', 1, 10, security_menu.created_by, security_menu.created_by
FROM security_menu;

UPDATE public.iam_permission_resource delivery
SET parent_id = directory.id,
    name = '短信列表',
    description = '按脱敏手机号和时间查看平台短信发送内容及投递结果',
    route_path = '/sms/deliveries',
    sort_order = 10,
    updated_by = directory.created_by,
    updated_time = CURRENT_TIMESTAMP,
    version = delivery.version + 1
FROM public.iam_permission_resource directory
WHERE delivery.permission_code = 'platform:sms-delivery:view'
  AND directory.permission_code = 'platform:sms:directory'
  AND delivery.is_deleted = false
  AND directory.is_deleted = false;

DO $$
DECLARE
    resource_count integer;
BEGIN
    SELECT COUNT(*) INTO resource_count
    FROM public.iam_permission_resource
    WHERE permission_code IN (
        'platform:sms:directory', 'platform:sms-delivery:view',
        'platform:sms-settings:view', 'platform:sms-settings:update',
        'platform:sms-security:view', 'platform:sms-security:update'
    )
      AND publish_status = 'PUBLISHED'
      AND is_deleted = false;
    IF resource_count <> 6 THEN
        RAISE EXCEPTION 'IAM_DML_0005 校验失败：有效短信管理权限数量=%', resource_count;
    END IF;
END
$$;

COMMIT;
