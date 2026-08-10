-- script_id: IAM_DML_0004
-- target_database: IAM
-- type: DML
-- depends_on: IAM_DML_0003
-- transactional: YES
-- purpose: 发布平台短信发送记录菜单权限
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
        RAISE EXCEPTION 'IAM_DML_0004 执行失败：不存在有效的平台身份';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.iam_permission_resource
        WHERE permission_code = 'platform:sms-delivery:view'
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0004 执行失败：短信记录权限码已存在，禁止覆盖、改义或复用';
    END IF;
END
$$;

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE'
      AND is_deleted = false
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
SELECT 'platform:sms-delivery:view',
       'MENU',
       NULL,
       '短信发送记录',
       '按脱敏手机号和时间查看平台短信发送内容及投递结果',
       'PLATFORM_ADMIN',
       '/sms-deliveries',
       NULL,
       NULL,
       NULL,
       NULL,
       'PUBLISHED',
       1,
       450,
       operator_identity.operator_id,
       operator_identity.operator_id
FROM operator_identity;

DO $$
DECLARE
    published_count integer;
BEGIN
    SELECT COUNT(*) INTO published_count
    FROM public.iam_permission_resource
    WHERE permission_code = 'platform:sms-delivery:view'
      AND resource_type = 'MENU'
      AND app_code = 'PLATFORM_ADMIN'
      AND route_path = '/sms-deliveries'
      AND publish_status = 'PUBLISHED'
      AND semantic_version = 1
      AND is_deleted = false;

    IF published_count <> 1 THEN
        RAISE EXCEPTION 'IAM_DML_0004 校验失败：已发布短信记录菜单数量=%', published_count;
    END IF;
END
$$;

COMMIT;

-- 执行后核对：应返回一条已发布的平台短信发送记录菜单。
-- SELECT permission_code, name, app_code, route_path, publish_status
-- FROM public.iam_permission_resource
-- WHERE permission_code = 'platform:sms-delivery:view';
