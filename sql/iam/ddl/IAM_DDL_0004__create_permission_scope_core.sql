-- script_id: IAM_DDL_0004
-- target_database: IAM
-- type: DDL
-- depends_on: IAM_DDL_0003
-- transactional: YES
-- purpose: 创建权限资源、角色权限、租户功能、菜单显示和角色数据范围结构
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.iam_permission_resource_seq AS bigint;
CREATE SEQUENCE public.iam_permission_api_binding_seq AS bigint;
CREATE SEQUENCE public.iam_role_permission_seq AS bigint;
CREATE SEQUENCE public.iam_tenant_feature_seq AS bigint;
CREATE SEQUENCE public.iam_tenant_menu_config_seq AS bigint;
CREATE SEQUENCE public.iam_account_menu_preference_seq AS bigint;
CREATE SEQUENCE public.iam_data_module_seq AS bigint;
CREATE SEQUENCE public.iam_role_data_policy_seq AS bigint;
CREATE SEQUENCE public.iam_role_data_scope_org_seq AS bigint;

CREATE TABLE public.iam_permission_resource (
    id bigint NOT NULL DEFAULT nextval('public.iam_permission_resource_seq'::regclass),
    permission_code varchar(128) NOT NULL,
    resource_type varchar(32) NOT NULL,
    parent_id bigint,
    name varchar(128) NOT NULL,
    description varchar(500),
    app_code varchar(32) NOT NULL,
    route_path varchar(256),
    component_key varchar(128),
    api_method varchar(16),
    api_path_pattern varchar(256),
    feature_code varchar(128),
    publish_status varchar(32) NOT NULL DEFAULT 'DRAFT',
    semantic_version integer NOT NULL DEFAULT 1,
    sort_order integer NOT NULL DEFAULT 0,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_permission_resource PRIMARY KEY (id),
    CONSTRAINT uk_iam_permission_resource_code UNIQUE (permission_code),
    CONSTRAINT fk_iam_permission_resource_parent
        FOREIGN KEY (parent_id) REFERENCES public.iam_permission_resource (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_permission_resource_not_self CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT ck_iam_permission_resource_type
        CHECK (resource_type IN ('DIRECTORY', 'MENU', 'BUTTON', 'API', 'DATA_MODULE')),
    CONSTRAINT ck_iam_permission_resource_app
        CHECK (app_code IN ('PLATFORM_ADMIN', 'TENANT_ADMIN', 'TENANT_PORTAL', 'SERVICE')),
    CONSTRAINT ck_iam_permission_resource_publish
        CHECK (publish_status IN ('DRAFT', 'PUBLISHED', 'DEPRECATED')),
    CONSTRAINT ck_iam_permission_resource_value
        CHECK (
            btrim(permission_code) <> ''
            AND btrim(name) <> ''
            AND (description IS NULL OR btrim(description) <> '')
            AND (route_path IS NULL OR btrim(route_path) <> '')
            AND (component_key IS NULL OR btrim(component_key) <> '')
            AND (feature_code IS NULL OR btrim(feature_code) <> '')
            AND semantic_version > 0
            AND version >= 0
        ),
    CONSTRAINT ck_iam_permission_resource_api
        CHECK (
            resource_type <> 'API'
            OR (
                api_method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE')
                AND api_path_pattern IS NOT NULL
                AND btrim(api_path_pattern) <> ''
            )
        )
);

ALTER SEQUENCE public.iam_permission_resource_seq
    OWNED BY public.iam_permission_resource.id;

CREATE INDEX idx_iam_permission_resource_tree
    ON public.iam_permission_resource (app_code, parent_id, sort_order, id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_permission_resource_publish
    ON public.iam_permission_resource (publish_status, resource_type, id)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX uk_iam_permission_resource_api
    ON public.iam_permission_resource (api_method, api_path_pattern)
    WHERE resource_type = 'API' AND is_deleted = false;

COMMENT ON TABLE public.iam_permission_resource IS '平台发布的菜单、按钮、API 和数据模块权限资源';
COMMENT ON COLUMN public.iam_permission_resource.permission_code IS '发布后永久保留且不得复用的权限标识';
COMMENT ON COLUMN public.iam_permission_resource.component_key IS '只能映射前端本地组件，禁止远程执行代码';
COMMENT ON COLUMN public.iam_permission_resource.publish_status IS '草稿、已发布或已废弃；已发布标识不得改义';

CREATE TABLE public.iam_permission_api_binding (
    id bigint NOT NULL DEFAULT nextval('public.iam_permission_api_binding_seq'::regclass),
    permission_id bigint NOT NULL,
    api_resource_id bigint NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_permission_api_binding PRIMARY KEY (id),
    CONSTRAINT fk_iam_permission_api_binding_permission
        FOREIGN KEY (permission_id) REFERENCES public.iam_permission_resource (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_permission_api_binding_api
        FOREIGN KEY (api_resource_id) REFERENCES public.iam_permission_resource (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_permission_api_binding_not_self CHECK (permission_id <> api_resource_id),
    CONSTRAINT ck_iam_permission_api_binding_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_permission_api_binding_seq
    OWNED BY public.iam_permission_api_binding.id;

CREATE UNIQUE INDEX uk_iam_permission_api_binding_active
    ON public.iam_permission_api_binding (permission_id, api_resource_id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_permission_api_binding IS '一个可授予业务权限绑定一个或多个真实 API 资源';

CREATE TABLE public.iam_role_permission (
    id bigint NOT NULL DEFAULT nextval('public.iam_role_permission_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    role_id bigint NOT NULL,
    permission_id bigint NOT NULL,
    granted_by bigint NOT NULL,
    granted_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_role_permission PRIMARY KEY (id),
    CONSTRAINT fk_iam_role_permission_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_role_permission_role
        FOREIGN KEY (tenant_id, organization_id, role_id)
        REFERENCES public.iam_role (tenant_id, organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_role_permission_resource
        FOREIGN KEY (permission_id) REFERENCES public.iam_permission_resource (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_role_permission_value CHECK (granted_by > 0 AND version >= 0)
);

ALTER SEQUENCE public.iam_role_permission_seq
    OWNED BY public.iam_role_permission.id;

CREATE UNIQUE INDEX uk_iam_role_permission_active
    ON public.iam_role_permission (role_id, permission_id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_role_permission_role
    ON public.iam_role_permission (tenant_id, organization_id, role_id, permission_id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_role_permission IS '角色功能权限；写入前必须校验不超过操作者授权上限';

CREATE TABLE public.iam_tenant_feature (
    id bigint NOT NULL DEFAULT nextval('public.iam_tenant_feature_seq'::regclass),
    tenant_id bigint NOT NULL,
    feature_code varchar(128) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    changed_by bigint NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_tenant_feature PRIMARY KEY (id),
    CONSTRAINT uk_iam_tenant_feature_code UNIQUE (tenant_id, feature_code),
    CONSTRAINT fk_iam_tenant_feature_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_tenant_feature_value
        CHECK (btrim(feature_code) <> '' AND changed_by > 0 AND version >= 0)
);

ALTER SEQUENCE public.iam_tenant_feature_seq
    OWNED BY public.iam_tenant_feature.id;

CREATE INDEX idx_iam_tenant_feature_enabled
    ON public.iam_tenant_feature (tenant_id, enabled, feature_code)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_tenant_feature IS '租户功能启停；停用功能会影响接口权限';

CREATE TABLE public.iam_tenant_menu_config (
    id bigint NOT NULL DEFAULT nextval('public.iam_tenant_menu_config_seq'::regclass),
    tenant_id bigint NOT NULL,
    menu_permission_id bigint NOT NULL,
    hidden boolean NOT NULL DEFAULT false,
    changed_by bigint NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_tenant_menu_config PRIMARY KEY (id),
    CONSTRAINT uk_iam_tenant_menu_config_menu UNIQUE (tenant_id, menu_permission_id),
    CONSTRAINT fk_iam_tenant_menu_config_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_tenant_menu_config_permission
        FOREIGN KEY (menu_permission_id) REFERENCES public.iam_permission_resource (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_tenant_menu_config_value CHECK (changed_by > 0 AND version >= 0)
);

ALTER SEQUENCE public.iam_tenant_menu_config_seq
    OWNED BY public.iam_tenant_menu_config.id;

COMMENT ON TABLE public.iam_tenant_menu_config IS '租户菜单显示配置；只影响导航显示，不改变接口权限';

CREATE TABLE public.iam_account_menu_preference (
    id bigint NOT NULL DEFAULT nextval('public.iam_account_menu_preference_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    account_id bigint NOT NULL,
    menu_permission_id bigint NOT NULL,
    hidden boolean NOT NULL DEFAULT false,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_account_menu_preference PRIMARY KEY (id),
    CONSTRAINT fk_iam_account_menu_preference_account
        FOREIGN KEY (tenant_id, organization_id, account_id)
        REFERENCES public.iam_account (tenant_id, organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_account_menu_preference_permission
        FOREIGN KEY (menu_permission_id) REFERENCES public.iam_permission_resource (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_account_menu_preference_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_account_menu_preference_seq
    OWNED BY public.iam_account_menu_preference.id;

CREATE UNIQUE INDEX uk_iam_account_menu_preference_active
    ON public.iam_account_menu_preference (account_id, menu_permission_id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_account_menu_preference IS '账号个人菜单隐藏偏好；不影响租户菜单和接口权限';

CREATE TABLE public.iam_data_module (
    id bigint NOT NULL DEFAULT nextval('public.iam_data_module_seq'::regclass),
    module_code varchar(128) NOT NULL,
    service_code varchar(128) NOT NULL,
    name varchar(128) NOT NULL,
    ownership_semantics varchar(500) NOT NULL,
    supported_scopes jsonb NOT NULL,
    publish_status varchar(32) NOT NULL DEFAULT 'DRAFT',
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_data_module PRIMARY KEY (id),
    CONSTRAINT uk_iam_data_module_code UNIQUE (module_code),
    CONSTRAINT ck_iam_data_module_value
        CHECK (
            btrim(module_code) <> ''
            AND btrim(service_code) <> ''
            AND btrim(name) <> ''
            AND btrim(ownership_semantics) <> ''
            AND jsonb_typeof(supported_scopes) = 'array'
            AND publish_status IN ('DRAFT', 'PUBLISHED', 'DEPRECATED')
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_data_module_seq
    OWNED BY public.iam_data_module.id;

CREATE INDEX idx_iam_data_module_service_publish
    ON public.iam_data_module (service_code, publish_status, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_data_module IS '平台发布的数据权限业务模块及真实数据归属语义';
COMMENT ON COLUMN public.iam_data_module.ownership_semantics IS '模块归属字段含义；未确认时不得发布模块';
COMMENT ON COLUMN public.iam_data_module.supported_scopes IS '模块允许的数据范围枚举 JSON 数组';

CREATE TABLE public.iam_role_data_policy (
    id bigint NOT NULL DEFAULT nextval('public.iam_role_data_policy_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    role_id bigint NOT NULL,
    module_code varchar(128) NOT NULL,
    scope_type varchar(64) NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_role_data_policy PRIMARY KEY (id),
    CONSTRAINT uk_iam_role_data_policy_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_iam_role_data_policy_role
        FOREIGN KEY (tenant_id, organization_id, role_id)
        REFERENCES public.iam_role (tenant_id, organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_role_data_policy_module CHECK (btrim(module_code) <> ''),
    CONSTRAINT ck_iam_role_data_policy_scope
        CHECK (
            scope_type IN (
                'SELF',
                'CURRENT_ORG',
                'CURRENT_ORG_AND_DESCENDANTS',
                'SPECIFIED_ORGS',
                'SPECIFIED_ORGS_AND_DESCENDANTS',
                'TENANT_ALL'
            )
        ),
    CONSTRAINT ck_iam_role_data_policy_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_role_data_policy_seq
    OWNED BY public.iam_role_data_policy.id;

CREATE UNIQUE INDEX uk_iam_role_data_policy_active
    ON public.iam_role_data_policy (role_id, module_code)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_role_data_policy_role
    ON public.iam_role_data_policy (tenant_id, organization_id, role_id, module_code)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_role_data_policy IS '角色默认数据范围和按业务模块覆盖范围';
COMMENT ON COLUMN public.iam_role_data_policy.module_code IS '星号表示角色默认范围，其它值表示已发布模块覆盖';

CREATE TABLE public.iam_role_data_scope_org (
    id bigint NOT NULL DEFAULT nextval('public.iam_role_data_scope_org_seq'::regclass),
    tenant_id bigint NOT NULL,
    policy_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_role_data_scope_org PRIMARY KEY (id),
    CONSTRAINT fk_iam_role_data_scope_org_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_role_data_scope_org_policy
        FOREIGN KEY (tenant_id, policy_id)
        REFERENCES public.iam_role_data_policy (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_role_data_scope_org_organization
        FOREIGN KEY (tenant_id, organization_id)
        REFERENCES public.iam_organization (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_role_data_scope_org_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_role_data_scope_org_seq
    OWNED BY public.iam_role_data_scope_org.id;

CREATE UNIQUE INDEX uk_iam_role_data_scope_org_active
    ON public.iam_role_data_scope_org (policy_id, organization_id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_role_data_scope_org IS '指定机构或指定机构及下级数据范围的机构集合';

COMMIT;
