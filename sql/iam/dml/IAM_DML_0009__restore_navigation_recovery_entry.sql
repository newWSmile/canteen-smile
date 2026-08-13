-- script_id: IAM_DML_0009
-- target_database: IAM
-- type: DML
-- depends_on: IAM_DML_0008
-- transactional: YES
-- purpose: 恢复被隐藏的租户功能与菜单治理入口并清理个人隐藏状态
-- rollback: 治理恢复入口属于防锁死保护，不提供回滚；后续仅允许新增更高编号修正脚本

BEGIN;

WITH navigation_menu AS (
    SELECT id
    FROM public.iam_permission_resource
    WHERE permission_code='iam:tenant-navigation:view'
      AND app_code='TENANT_ADMIN'
      AND resource_type='MENU'
      AND publish_status='PUBLISHED'
      AND is_deleted=false
)
UPDATE public.iam_tenant_menu_config config
SET hidden=false,
    updated_time=CURRENT_TIMESTAMP,
    version=config.version+1
FROM navigation_menu
WHERE config.menu_permission_id=navigation_menu.id
  AND config.hidden=true
  AND config.is_deleted=false;

WITH navigation_menu AS (
    SELECT id
    FROM public.iam_permission_resource
    WHERE permission_code='iam:tenant-navigation:view'
      AND app_code='TENANT_ADMIN'
      AND resource_type='MENU'
      AND publish_status='PUBLISHED'
      AND is_deleted=false
)
UPDATE public.iam_account_menu_preference preference
SET hidden=false,
    updated_by=preference.account_id,
    updated_time=CURRENT_TIMESTAMP,
    version=preference.version+1
FROM navigation_menu
WHERE preference.menu_permission_id=navigation_menu.id
  AND preference.hidden=true
  AND preference.is_deleted=false;

DO $$
DECLARE
    hidden_tenant_count integer;
    hidden_preference_count integer;
BEGIN
    SELECT COUNT(*) INTO hidden_tenant_count
    FROM public.iam_tenant_menu_config config
    JOIN public.iam_permission_resource permission ON permission.id=config.menu_permission_id
    WHERE permission.permission_code='iam:tenant-navigation:view'
      AND config.hidden=true
      AND config.is_deleted=false;

    SELECT COUNT(*) INTO hidden_preference_count
    FROM public.iam_account_menu_preference preference
    JOIN public.iam_permission_resource permission ON permission.id=preference.menu_permission_id
    WHERE permission.permission_code='iam:tenant-navigation:view'
      AND preference.hidden=true
      AND preference.is_deleted=false;

    IF hidden_tenant_count <> 0 OR hidden_preference_count <> 0 THEN
        RAISE EXCEPTION 'IAM_DML_0009 校验失败：租户隐藏数=%, 个人隐藏数=%',
          hidden_tenant_count,hidden_preference_count;
    END IF;
END
$$;

COMMIT;

-- 执行后核对：以下查询应返回 0 行。
-- SELECT permission.permission_code,config.tenant_id,config.hidden
-- FROM public.iam_tenant_menu_config config
-- JOIN public.iam_permission_resource permission ON permission.id=config.menu_permission_id
-- WHERE permission.permission_code='iam:tenant-navigation:view'
--   AND config.hidden=true AND config.is_deleted=false;
