-- script_id: IAM_DML_0001
-- target_database: IAM
-- type: DML
-- depends_on: IAM_DDL_0006
-- transactional: YES
-- purpose: 正式发布租户管理端现有四个菜单及其已实现按钮权限，并为已有租户初始化菜单显示配置
-- rollback: 权限标识发布后永久保留且禁止复用；执行后不允许删除或回滚，只能新增更高编号修正脚本

BEGIN;

-- 发布动作必须有真实、有效的平台身份作为审计操作人，不使用约定 ID 或伪造系统账号。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM public.iam_platform_identity
        WHERE status = 'ACTIVE'
          AND is_deleted = false
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0001 执行失败：不存在有效的平台身份，请先完成平台超级管理员初始化';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.iam_permission_resource
        WHERE permission_code IN (
            'iam:org-type:view',
            'iam:org-type:manage',
            'iam:org:view',
            'iam:org:create',
            'iam:org:update',
            'iam:org:move',
            'iam:org:status',
            'iam:org:delete',
            'iam:role:view',
            'iam:role:create',
            'iam:role:update',
            'iam:role:status',
            'iam:role:delete',
            'iam:role:grant',
            'iam:role:data-scope',
            'iam:user:view',
            'iam:user:create',
            'iam:user:update',
            'iam:user:status',
            'iam:user:cancel',
            'iam:user:role-assign'
        )
    ) THEN
        RAISE EXCEPTION 'IAM_DML_0001 执行失败：待发布权限码已存在。发布权限码禁止覆盖、改义或复用，请先核对执行记录';
    END IF;
END
$$;

-- 发布四个租户管理端菜单。路由均来自当前 tenant-admin 的真实路由配置。
WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE'
      AND is_deleted = false
), menu_resource (
    permission_code,
    name,
    description,
    route_path,
    sort_order
) AS (
    VALUES
        ('iam:org-type:view', '机构类型与关系', '查看并进入租户机构类型与允许关系页面', '/organization-types', 100),
        ('iam:org:view', '机构树', '查看并进入当前权限范围内的机构树页面', '/organizations', 200),
        ('iam:role:view', '角色与授权', '查看并进入本机构角色与授权页面', '/roles', 300),
        ('iam:user:view', '用户管理', '查看并进入本机构用户管理页面', '/users', 400)
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
SELECT menu_resource.permission_code,
       'MENU',
       NULL,
       menu_resource.name,
       menu_resource.description,
       'TENANT_ADMIN',
       menu_resource.route_path,
       NULL,
       NULL,
       NULL,
       NULL,
       'PUBLISHED',
       1,
       menu_resource.sort_order,
       operator_identity.operator_id,
       operator_identity.operator_id
FROM menu_resource
CROSS JOIN operator_identity;

-- 发布页面已经实现且后端已经使用的十七个按钮权限，并挂到对应菜单节点下。
WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE'
      AND is_deleted = false
), button_resource (
    permission_code,
    parent_permission_code,
    name,
    description,
    sort_order
) AS (
    VALUES
        ('iam:org-type:manage', 'iam:org-type:view', '维护机构类型与关系', '新增、修改、停用机构类型并维护允许的父子类型关系', 110),
        ('iam:org:create', 'iam:org:view', '新增机构', '在授权范围内新增合法的下级机构', 210),
        ('iam:org:update', 'iam:org:view', '修改机构', '修改授权范围内机构的基础资料或合法机构类型', 220),
        ('iam:org:move', 'iam:org:view', '迁移机构', '在同一租户内将机构迁移到合法父机构', 230),
        ('iam:org:status', 'iam:org:view', '变更机构状态', '停用或恢复授权范围内机构及其状态继承关系', 240),
        ('iam:org:delete', 'iam:org:view', '删除空白机构', '删除从未承载业务数据的空白机构', 250),
        ('iam:role:create', 'iam:role:view', '新增角色', '在本机构新增自定义角色', 310),
        ('iam:role:update', 'iam:role:view', '修改角色', '修改本机构自定义角色的基础资料', 320),
        ('iam:role:status', 'iam:role:view', '变更角色状态', '停用或恢复本机构自定义角色', 330),
        ('iam:role:delete', 'iam:role:view', '删除角色', '删除本机构自定义角色并永久保留角色编码和历史 ID', 340),
        ('iam:role:grant', 'iam:role:view', '分配功能权限', '为本机构角色分配操作者自身拥有的已发布功能权限', 350),
        ('iam:role:data-scope', 'iam:role:view', '配置数据范围', '配置本机构角色默认数据范围及业务模块覆盖范围', 360),
        ('iam:user:create', 'iam:user:view', '新增用户', '在本机构新增待激活用户并生成账号激活链接', 410),
        ('iam:user:update', 'iam:user:view', '修改用户', '修改本机构普通用户的基础资料', 420),
        ('iam:user:status', 'iam:user:view', '变更用户状态', '停用或恢复本机构普通用户账号', 430),
        ('iam:user:cancel', 'iam:user:view', '注销用户', '不可恢复地注销本机构普通用户账号', 440),
        ('iam:user:role-assign', 'iam:user:view', '分配用户角色', '替换本机构普通用户的角色集合', 450)
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
SELECT button_resource.permission_code,
       'BUTTON',
       parent_resource.id,
       button_resource.name,
       button_resource.description,
       'TENANT_ADMIN',
       NULL,
       NULL,
       NULL,
       NULL,
       NULL,
       'PUBLISHED',
       1,
       button_resource.sort_order,
       operator_identity.operator_id,
       operator_identity.operator_id
FROM button_resource
JOIN public.iam_permission_resource parent_resource
  ON parent_resource.permission_code = button_resource.parent_permission_code
 AND parent_resource.resource_type = 'MENU'
 AND parent_resource.app_code = 'TENANT_ADMIN'
 AND parent_resource.publish_status = 'PUBLISHED'
 AND parent_resource.is_deleted = false
CROSS JOIN operator_identity;

-- 为已创建租户补齐四个菜单的显示配置；未来新租户仍由租户开通流程自动初始化。
WITH operator_identity AS (
    SELECT MIN(id) AS operator_id
    FROM public.iam_platform_identity
    WHERE status = 'ACTIVE'
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
       menu_resource.id,
       false,
       operator_identity.operator_id,
       operator_identity.operator_id,
       operator_identity.operator_id
FROM public.iam_tenant tenant
JOIN public.iam_permission_resource menu_resource
  ON menu_resource.permission_code IN (
      'iam:org-type:view',
      'iam:org:view',
      'iam:role:view',
      'iam:user:view'
  )
 AND menu_resource.resource_type = 'MENU'
 AND menu_resource.app_code = 'TENANT_ADMIN'
 AND menu_resource.publish_status = 'PUBLISHED'
 AND menu_resource.is_deleted = false
CROSS JOIN operator_identity
WHERE tenant.is_deleted = false
ON CONFLICT (tenant_id, menu_permission_id) DO NOTHING;

-- 事务提交前核对资源总数、类型、发布状态和按钮父子关系，任一不符合即整体回滚。
DO $$
DECLARE
    published_resource_count integer;
    published_menu_count integer;
    published_button_count integer;
    invalid_button_parent_count integer;
BEGIN
    SELECT COUNT(*) INTO published_resource_count
    FROM public.iam_permission_resource
    WHERE permission_code IN (
        'iam:org-type:view',
        'iam:org-type:manage',
        'iam:org:view',
        'iam:org:create',
        'iam:org:update',
        'iam:org:move',
        'iam:org:status',
        'iam:org:delete',
        'iam:role:view',
        'iam:role:create',
        'iam:role:update',
        'iam:role:status',
        'iam:role:delete',
        'iam:role:grant',
        'iam:role:data-scope',
        'iam:user:view',
        'iam:user:create',
        'iam:user:update',
        'iam:user:status',
        'iam:user:cancel',
        'iam:user:role-assign'
    )
      AND app_code = 'TENANT_ADMIN'
      AND publish_status = 'PUBLISHED'
      AND semantic_version = 1
      AND is_deleted = false;

    SELECT COUNT(*) INTO published_menu_count
    FROM public.iam_permission_resource
    WHERE permission_code IN ('iam:org-type:view', 'iam:org:view', 'iam:role:view', 'iam:user:view')
      AND resource_type = 'MENU'
      AND app_code = 'TENANT_ADMIN'
      AND publish_status = 'PUBLISHED'
      AND parent_id IS NULL
      AND is_deleted = false;

    SELECT COUNT(*) INTO published_button_count
    FROM public.iam_permission_resource
    WHERE permission_code IN (
        'iam:org-type:manage',
        'iam:org:create',
        'iam:org:update',
        'iam:org:move',
        'iam:org:status',
        'iam:org:delete',
        'iam:role:create',
        'iam:role:update',
        'iam:role:status',
        'iam:role:delete',
        'iam:role:grant',
        'iam:role:data-scope',
        'iam:user:create',
        'iam:user:update',
        'iam:user:status',
        'iam:user:cancel',
        'iam:user:role-assign'
    )
      AND resource_type = 'BUTTON'
      AND app_code = 'TENANT_ADMIN'
      AND publish_status = 'PUBLISHED'
      AND is_deleted = false;

    SELECT COUNT(*) INTO invalid_button_parent_count
    FROM public.iam_permission_resource button_resource
    LEFT JOIN public.iam_permission_resource menu_resource
      ON menu_resource.id = button_resource.parent_id
    WHERE button_resource.permission_code IN (
        'iam:org-type:manage',
        'iam:org:create',
        'iam:org:update',
        'iam:org:move',
        'iam:org:status',
        'iam:org:delete',
        'iam:role:create',
        'iam:role:update',
        'iam:role:status',
        'iam:role:delete',
        'iam:role:grant',
        'iam:role:data-scope',
        'iam:user:create',
        'iam:user:update',
        'iam:user:status',
        'iam:user:cancel',
        'iam:user:role-assign'
    )
      AND menu_resource.permission_code IS DISTINCT FROM CASE
          WHEN button_resource.permission_code LIKE 'iam:org-type:%' THEN 'iam:org-type:view'
          WHEN button_resource.permission_code LIKE 'iam:org:%' THEN 'iam:org:view'
          WHEN button_resource.permission_code LIKE 'iam:role:%' THEN 'iam:role:view'
          WHEN button_resource.permission_code LIKE 'iam:user:%' THEN 'iam:user:view'
      END;

    IF published_resource_count <> 21
       OR published_menu_count <> 4
       OR published_button_count <> 17
       OR invalid_button_parent_count <> 0 THEN
        RAISE EXCEPTION
            'IAM_DML_0001 校验失败：资源=%, 菜单=%, 按钮=%, 错误父节点=%',
            published_resource_count,
            published_menu_count,
            published_button_count,
            invalid_button_parent_count;
    END IF;
END
$$;

COMMIT;

-- 执行后影响行数核对：第一条应返回 MENU=4、BUTTON=17；第二条每个有效租户应返回 menu_count=4。
-- SELECT resource_type, COUNT(*) AS resource_count
-- FROM public.iam_permission_resource
-- WHERE app_code = 'TENANT_ADMIN' AND publish_status = 'PUBLISHED' AND semantic_version = 1
--   AND permission_code LIKE 'iam:%' AND is_deleted = false
-- GROUP BY resource_type ORDER BY resource_type;
--
-- SELECT tenant.id AS tenant_id, COUNT(menu_resource.id) AS menu_count
-- FROM public.iam_tenant tenant
-- LEFT JOIN public.iam_tenant_menu_config menu_config
--   ON menu_config.tenant_id = tenant.id AND menu_config.is_deleted = false
-- LEFT JOIN public.iam_permission_resource menu_resource
--   ON menu_resource.id = menu_config.menu_permission_id
--  AND menu_resource.permission_code IN ('iam:org-type:view', 'iam:org:view', 'iam:role:view', 'iam:user:view')
-- WHERE tenant.is_deleted = false
-- GROUP BY tenant.id ORDER BY tenant.id;
