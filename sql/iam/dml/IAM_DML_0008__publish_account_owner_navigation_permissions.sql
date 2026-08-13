-- script_id: IAM_DML_0008
-- target_database: IAM
-- type: DML
-- depends_on: IAM_DML_0007
-- transactional: YES
-- purpose: 发布用户名、密码重置、机构所有权和租户导航治理权限，并为既有租户补齐功能与菜单配置
-- rollback: 权限码发布后永久保留且禁止复用；执行后不得回滚，只能新增更高编号脚本修正

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.iam_platform_identity WHERE status='ACTIVE' AND is_deleted=false
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0008 执行失败：不存在有效的平台身份';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.iam_permission_resource
        WHERE permission_code IN (
            'iam:user:password-reset','iam:org-owner:view','iam:org-owner:transfer',
            'iam:tenant-navigation:view','iam:tenant-navigation:manage'
        )
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0008 执行失败：待发布权限码已存在，禁止覆盖、改义或复用';
    END IF;
END
$$;

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status='ACTIVE' AND is_deleted=false
), parent_menu AS (
    SELECT id, permission_code
    FROM public.iam_permission_resource
    WHERE permission_code IN ('iam:user:view') AND publish_status='PUBLISHED' AND is_deleted=false
), button_resource(permission_code,name,description,parent_code,api_method,api_path_pattern,feature_code,sort_order) AS (
    VALUES
      ('iam:user:password-reset','重置用户密码','为本机构授权范围内账号生成三十分钟一次性密码重置链接','iam:user:view','POST','/api/iam/v1/tenant/users/{accountId}/password-reset-links','IAM_USER',80),
      ('iam:org-owner:view','查看机构所有者','查看当前机构唯一所有者及所有权版本','iam:user:view','GET','/api/iam/v1/tenant/organization-owner',NULL,90),
      ('iam:org-owner:transfer','转让机构所有权','当前机构所有者经再认证后将唯一所有权转让给本机构合格账号','iam:user:view','POST','/api/iam/v1/tenant/organization-owner/actions/transfer',NULL,100)
)
INSERT INTO public.iam_permission_resource (
    permission_code,resource_type,parent_id,name,description,app_code,route_path,component_key,
    api_method,api_path_pattern,feature_code,publish_status,semantic_version,sort_order,created_by,updated_by
)
SELECT resource.permission_code,'BUTTON',parent_menu.id,resource.name,resource.description,'TENANT_ADMIN',
       NULL,NULL,resource.api_method,resource.api_path_pattern,resource.feature_code,'PUBLISHED',1,
       resource.sort_order,operator_identity.operator_id,operator_identity.operator_id
FROM button_resource resource
JOIN parent_menu ON parent_menu.permission_code=resource.parent_code
CROSS JOIN operator_identity;

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status='ACTIVE' AND is_deleted=false
), navigation_menu AS (
    INSERT INTO public.iam_permission_resource (
        permission_code,resource_type,parent_id,name,description,app_code,route_path,component_key,
        api_method,api_path_pattern,feature_code,publish_status,semantic_version,sort_order,created_by,updated_by
    )
    SELECT 'iam:tenant-navigation:view','MENU',NULL,'功能与菜单','维护租户功能启停和全部机构统一菜单显示',
           'TENANT_ADMIN','/tenant/navigation',NULL,NULL,NULL,NULL,'PUBLISHED',1,650,
           operator_id,operator_id
    FROM operator_identity
    RETURNING id,created_by
)
INSERT INTO public.iam_permission_resource (
    permission_code,resource_type,parent_id,name,description,app_code,route_path,component_key,
    api_method,api_path_pattern,feature_code,publish_status,semantic_version,sort_order,created_by,updated_by
)
SELECT 'iam:tenant-navigation:manage','BUTTON',id,'维护功能与菜单',
       '租户根机构所有者填写原因并完成密码再认证后修改功能启停或菜单显示',
       'TENANT_ADMIN',NULL,NULL,'PUT','/api/iam/v1/tenant/navigation-settings/**',NULL,
       'PUBLISHED',1,10,created_by,created_by
FROM navigation_menu;

-- 将已发布权限归入稳定功能编码；租户功能停用由后端最终权限查询执行。
UPDATE public.iam_permission_resource
SET feature_code = CASE
      WHEN permission_code LIKE 'iam:org-type:%' THEN 'IAM_ORGANIZATION_TYPE'
      WHEN permission_code LIKE 'iam:org:%' THEN 'IAM_ORGANIZATION'
      WHEN permission_code LIKE 'iam:role:%' THEN 'IAM_ROLE'
      WHEN permission_code LIKE 'iam:user:%' THEN 'IAM_USER'
      WHEN permission_code LIKE 'iam:audit:%' THEN 'IAM_AUDIT'
      WHEN permission_code LIKE 'iam:tenant-security:%' THEN 'IAM_TENANT_SECURITY'
      ELSE feature_code
    END,
    updated_time=CURRENT_TIMESTAMP,
    version=version+1
WHERE app_code='TENANT_ADMIN' AND publish_status='PUBLISHED' AND is_deleted=false
  AND feature_code IS NULL
  AND (
    permission_code LIKE 'iam:org-type:%' OR permission_code LIKE 'iam:org:%'
    OR permission_code LIKE 'iam:role:%' OR permission_code LIKE 'iam:user:%'
    OR permission_code LIKE 'iam:audit:%' OR permission_code LIKE 'iam:tenant-security:%'
  )
  AND permission_code NOT LIKE 'iam:org-owner:%';

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id FROM public.iam_platform_identity
    WHERE status='ACTIVE' AND is_deleted=false
), feature(feature_code) AS (
    VALUES ('IAM_ORGANIZATION_TYPE'),('IAM_ORGANIZATION'),('IAM_ROLE'),
           ('IAM_USER'),('IAM_AUDIT'),('IAM_TENANT_SECURITY')
)
INSERT INTO public.iam_tenant_feature (
    tenant_id,feature_code,enabled,changed_by,created_by,updated_by
)
SELECT tenant.id,feature.feature_code,true,operator_identity.operator_id,
       operator_identity.operator_id,operator_identity.operator_id
FROM public.iam_tenant tenant
CROSS JOIN feature
CROSS JOIN operator_identity
WHERE tenant.is_deleted=false
ON CONFLICT (tenant_id,feature_code) DO NOTHING;

WITH operator_identity AS (
    SELECT MIN(id) AS operator_id FROM public.iam_platform_identity
    WHERE status='ACTIVE' AND is_deleted=false
), navigation_menu AS (
    SELECT id FROM public.iam_permission_resource
    WHERE permission_code='iam:tenant-navigation:view' AND publish_status='PUBLISHED' AND is_deleted=false
)
INSERT INTO public.iam_tenant_menu_config (
    tenant_id,menu_permission_id,hidden,changed_by,created_by,updated_by
)
SELECT tenant.id,navigation_menu.id,false,operator_identity.operator_id,
       operator_identity.operator_id,operator_identity.operator_id
FROM public.iam_tenant tenant
CROSS JOIN navigation_menu
CROSS JOIN operator_identity
WHERE tenant.is_deleted=false
ON CONFLICT (tenant_id,menu_permission_id) DO NOTHING;

DO $$
DECLARE
    permission_count integer;
    missing_feature_count integer;
    missing_menu_count integer;
BEGIN
    SELECT COUNT(*) INTO permission_count
    FROM public.iam_permission_resource
    WHERE permission_code IN (
      'iam:user:password-reset','iam:org-owner:view','iam:org-owner:transfer',
      'iam:tenant-navigation:view','iam:tenant-navigation:manage'
    ) AND publish_status='PUBLISHED' AND is_deleted=false;

    SELECT COUNT(*) INTO missing_feature_count
    FROM public.iam_tenant tenant
    CROSS JOIN (VALUES ('IAM_ORGANIZATION_TYPE'),('IAM_ORGANIZATION'),('IAM_ROLE'),
      ('IAM_USER'),('IAM_AUDIT'),('IAM_TENANT_SECURITY')) feature(feature_code)
    WHERE tenant.is_deleted=false AND NOT EXISTS (
      SELECT 1 FROM public.iam_tenant_feature configured
      WHERE configured.tenant_id=tenant.id AND configured.feature_code=feature.feature_code
        AND configured.is_deleted=false
    );

    SELECT COUNT(*) INTO missing_menu_count
    FROM public.iam_tenant tenant
    WHERE tenant.is_deleted=false AND NOT EXISTS (
      SELECT 1 FROM public.iam_tenant_menu_config config
      JOIN public.iam_permission_resource permission ON permission.id=config.menu_permission_id
      WHERE config.tenant_id=tenant.id AND permission.permission_code='iam:tenant-navigation:view'
        AND config.is_deleted=false
    );

    IF permission_count <> 5 OR missing_feature_count <> 0 OR missing_menu_count <> 0 THEN
      RAISE EXCEPTION 'IAM_DML_0008 校验失败：权限数=%, 缺失功能配置=%, 缺失菜单配置=%',
        permission_count,missing_feature_count,missing_menu_count;
    END IF;
END
$$;

COMMIT;

-- 执行后核对：应返回 5 个新权限；每个有效租户应具备 6 个功能配置和“功能与菜单”显示配置。
-- SELECT permission_code,resource_type,parent_id,feature_code,publish_status
-- FROM public.iam_permission_resource
-- WHERE permission_code IN ('iam:user:password-reset','iam:org-owner:view','iam:org-owner:transfer',
--   'iam:tenant-navigation:view','iam:tenant-navigation:manage');
