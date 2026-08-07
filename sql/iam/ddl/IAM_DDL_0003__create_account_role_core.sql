-- script_id: IAM_DDL_0003
-- target_database: IAM
-- type: DDL
-- depends_on: IAM_DDL_0001, IAM_DDL_0002
-- transactional: YES
-- purpose: 创建租户账号、工号永久保留、角色、用户角色、机构所有者和所有权转让历史
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.iam_account_seq AS bigint;
CREATE SEQUENCE public.iam_employee_number_registry_seq AS bigint;
CREATE SEQUENCE public.iam_role_seq AS bigint;
CREATE SEQUENCE public.iam_account_role_seq AS bigint;
CREATE SEQUENCE public.iam_org_owner_seq AS bigint;
CREATE SEQUENCE public.iam_org_owner_history_seq AS bigint;

CREATE TABLE public.iam_account (
    id bigint NOT NULL DEFAULT nextval('public.iam_account_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    username varchar(128) NOT NULL,
    normalized_username varchar(128) NOT NULL,
    display_name varchar(128),
    employee_number varchar(64),
    status varchar(32) NOT NULL DEFAULT 'PENDING_ACTIVATION',
    validity_mode varchar(32) NOT NULL DEFAULT 'LONG_TERM',
    effective_at timestamptz,
    expires_at timestamptz,
    authz_version bigint NOT NULL DEFAULT 0,
    profile_version bigint NOT NULL DEFAULT 0,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_account PRIMARY KEY (id),
    CONSTRAINT uk_iam_account_tenant_org_id UNIQUE (tenant_id, organization_id, id),
    CONSTRAINT fk_iam_account_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_account_organization
        FOREIGN KEY (tenant_id, organization_id)
        REFERENCES public.iam_organization (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_account_username_not_blank
        CHECK (btrim(username) <> '' AND btrim(normalized_username) <> ''),
    CONSTRAINT ck_iam_account_optional_value
        CHECK (
            (display_name IS NULL OR btrim(display_name) <> '')
            AND (employee_number IS NULL OR btrim(employee_number) <> '')
        ),
    CONSTRAINT ck_iam_account_status
        CHECK (
            status IN (
                'PENDING_ACTIVATION',
                'ACTIVE',
                'PASSWORD_RESET_REQUIRED',
                'DISABLED',
                'CANCELLED'
            )
        ),
    CONSTRAINT ck_iam_account_validity_mode
        CHECK (validity_mode IN ('LONG_TERM', 'FIXED_PERIOD')),
    CONSTRAINT ck_iam_account_validity_period
        CHECK (
            (
                validity_mode = 'LONG_TERM'
                AND effective_at IS NULL
                AND expires_at IS NULL
            )
            OR (
                validity_mode = 'FIXED_PERIOD'
                AND effective_at IS NOT NULL
                AND expires_at IS NOT NULL
                AND effective_at < expires_at
            )
        ),
    CONSTRAINT ck_iam_account_version_non_negative
        CHECK (authz_version >= 0 AND profile_version >= 0 AND version >= 0)
);

ALTER SEQUENCE public.iam_account_seq
    OWNED BY public.iam_account.id;

CREATE UNIQUE INDEX uk_iam_account_normalized_username
    ON public.iam_account (normalized_username)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_account_tenant_org_status
    ON public.iam_account (tenant_id, organization_id, status, id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_account_validity_expiration
    ON public.iam_account (tenant_id, expires_at, id)
    WHERE validity_mode = 'FIXED_PERIOD' AND is_deleted = false;

COMMENT ON TABLE public.iam_account IS '租户账号资料；认证秘密由 Auth 服务独占';
COMMENT ON COLUMN public.iam_account.organization_id IS '账号唯一所属机构，创建后禁止直接更换';
COMMENT ON COLUMN public.iam_account.username IS '当前原始用户名；所有曾用值由全局用户名注册表永久保留';
COMMENT ON COLUMN public.iam_account.normalized_username IS '服务端归一化后的当前用户名';
COMMENT ON COLUMN public.iam_account.display_name IS '可选显示名称，为空时展示用户名';
COMMENT ON COLUMN public.iam_account.employee_number IS '可选工号，永久唯一性由工号注册表保证';
COMMENT ON COLUMN public.iam_account.status IS 'IAM 账号状态；密码登录临时锁定由 Auth 独立维护';
COMMENT ON COLUMN public.iam_account.validity_mode IS '长期有效或指定生效和到期时间';
COMMENT ON COLUMN public.iam_account.authz_version IS '角色或授权变化时提升并使全部设备会话失效';
COMMENT ON COLUMN public.iam_account.profile_version IS '用户名和安全资料版本';

CREATE TABLE public.iam_employee_number_registry (
    id bigint NOT NULL DEFAULT nextval('public.iam_employee_number_registry_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    normalized_employee_number varchar(64) NOT NULL,
    account_id bigint NOT NULL,
    original_employee_number varchar(64) NOT NULL,
    reserved_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_employee_number_registry PRIMARY KEY (id),
    CONSTRAINT uk_iam_employee_number_registry_number
        UNIQUE (tenant_id, organization_id, normalized_employee_number),
    CONSTRAINT ck_iam_employee_number_registry_value
        CHECK (
            btrim(normalized_employee_number) <> ''
            AND btrim(original_employee_number) <> ''
            AND account_id > 0
            AND is_deleted = false
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_employee_number_registry_seq
    OWNED BY public.iam_employee_number_registry.id;

CREATE INDEX idx_iam_employee_number_registry_account
    ON public.iam_employee_number_registry (tenant_id, organization_id, account_id, reserved_time DESC);

COMMENT ON TABLE public.iam_employee_number_registry IS '机构内所有使用过的工号永久保留记录';
COMMENT ON COLUMN public.iam_employee_number_registry.tenant_id IS '首次占用工号的租户 ID，不设置外键以保留历史';
COMMENT ON COLUMN public.iam_employee_number_registry.organization_id IS '首次占用工号的机构 ID，不设置外键以保留历史';
COMMENT ON COLUMN public.iam_employee_number_registry.account_id IS '首次占用该工号的账号 ID，不设置外键以保留历史';

CREATE TABLE public.iam_role (
    id bigint NOT NULL DEFAULT nextval('public.iam_role_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    role_code varchar(64) NOT NULL,
    name varchar(128) NOT NULL,
    normalized_name varchar(128) NOT NULL,
    description varchar(500),
    role_type varchar(32) NOT NULL DEFAULT 'CUSTOM',
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    authz_version bigint NOT NULL DEFAULT 0,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_role PRIMARY KEY (id),
    CONSTRAINT uk_iam_role_code UNIQUE (role_code),
    CONSTRAINT uk_iam_role_tenant_org_id UNIQUE (tenant_id, organization_id, id),
    CONSTRAINT fk_iam_role_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_role_organization
        FOREIGN KEY (tenant_id, organization_id)
        REFERENCES public.iam_organization (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_role_value
        CHECK (
            btrim(role_code) <> ''
            AND btrim(name) <> ''
            AND btrim(normalized_name) <> ''
            AND (description IS NULL OR btrim(description) <> '')
        ),
    CONSTRAINT ck_iam_role_type CHECK (role_type IN ('OWNER', 'CUSTOM')),
    CONSTRAINT ck_iam_role_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_iam_role_version_non_negative CHECK (authz_version >= 0 AND version >= 0)
);

ALTER SEQUENCE public.iam_role_seq
    OWNED BY public.iam_role.id;

CREATE UNIQUE INDEX uk_iam_role_org_name
    ON public.iam_role (tenant_id, organization_id, normalized_name)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX uk_iam_role_org_owner_type
    ON public.iam_role (tenant_id, organization_id)
    WHERE role_type = 'OWNER' AND is_deleted = false;

CREATE INDEX idx_iam_role_org_status
    ON public.iam_role (tenant_id, organization_id, status, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_role IS '机构角色；同一账号可绑定多个角色并取权限并集';
COMMENT ON COLUMN public.iam_role.role_code IS '系统生成且永久保留的稳定角色编码';
COMMENT ON COLUMN public.iam_role.normalized_name IS '用于同机构当前角色名称唯一校验';
COMMENT ON COLUMN public.iam_role.role_type IS '机构所有者保护角色或自定义角色';
COMMENT ON COLUMN public.iam_role.status IS '角色启停状态；删除使用 is_deleted 且保留历史 ID 和编码';
COMMENT ON COLUMN public.iam_role.authz_version IS '权限或数据范围变化时提升的角色授权版本';

CREATE TABLE public.iam_account_role (
    id bigint NOT NULL DEFAULT nextval('public.iam_account_role_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    account_id bigint NOT NULL,
    role_id bigint NOT NULL,
    assigned_by bigint NOT NULL,
    assigned_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_account_role PRIMARY KEY (id),
    CONSTRAINT fk_iam_account_role_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_account_role_account
        FOREIGN KEY (tenant_id, organization_id, account_id)
        REFERENCES public.iam_account (tenant_id, organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_account_role_role
        FOREIGN KEY (tenant_id, organization_id, role_id)
        REFERENCES public.iam_role (tenant_id, organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_account_role_value CHECK (assigned_by > 0 AND version >= 0)
);

ALTER SEQUENCE public.iam_account_role_seq
    OWNED BY public.iam_account_role.id;

CREATE UNIQUE INDEX uk_iam_account_role_active_binding
    ON public.iam_account_role (account_id, role_id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_account_role_account
    ON public.iam_account_role (tenant_id, organization_id, account_id, role_id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_account_role_role
    ON public.iam_account_role (tenant_id, organization_id, role_id, account_id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_account_role IS '账号与角色绑定；功能权限和数据范围按有效角色取并集';

CREATE TABLE public.iam_org_owner (
    id bigint NOT NULL DEFAULT nextval('public.iam_org_owner_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    account_id bigint NOT NULL,
    protected_role_id bigint NOT NULL,
    effective_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_org_owner PRIMARY KEY (id),
    CONSTRAINT fk_iam_org_owner_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_owner_organization
        FOREIGN KEY (tenant_id, organization_id)
        REFERENCES public.iam_organization (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_owner_account
        FOREIGN KEY (tenant_id, organization_id, account_id)
        REFERENCES public.iam_account (tenant_id, organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_owner_role
        FOREIGN KEY (tenant_id, organization_id, protected_role_id)
        REFERENCES public.iam_role (tenant_id, organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_org_owner_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_org_owner_seq
    OWNED BY public.iam_org_owner.id;

CREATE UNIQUE INDEX uk_iam_org_owner_active_org
    ON public.iam_org_owner (tenant_id, organization_id)
    WHERE is_deleted = false;

CREATE UNIQUE INDEX uk_iam_org_owner_active_account
    ON public.iam_org_owner (tenant_id, account_id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_org_owner IS '每个机构唯一有效所有者及其受保护 OWNER 角色';
COMMENT ON COLUMN public.iam_org_owner.protected_role_id IS '机构创建时同步创建且普通管理员不能修改的所有者角色';

CREATE TABLE public.iam_org_owner_history (
    id bigint NOT NULL DEFAULT nextval('public.iam_org_owner_history_seq'::regclass),
    tenant_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    from_account_id bigint,
    to_account_id bigint NOT NULL,
    reason varchar(500) NOT NULL,
    operator_id bigint NOT NULL,
    changed_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_org_owner_history PRIMARY KEY (id),
    CONSTRAINT fk_iam_org_owner_history_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_owner_history_organization
        FOREIGN KEY (tenant_id, organization_id)
        REFERENCES public.iam_organization (tenant_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_owner_history_from_account
        FOREIGN KEY (tenant_id, organization_id, from_account_id)
        REFERENCES public.iam_account (tenant_id, organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_org_owner_history_to_account
        FOREIGN KEY (tenant_id, organization_id, to_account_id)
        REFERENCES public.iam_account (tenant_id, organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_org_owner_history_value
        CHECK (
            (from_account_id IS NULL OR from_account_id <> to_account_id)
            AND btrim(reason) <> ''
            AND operator_id > 0
            AND is_deleted = false
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_org_owner_history_seq
    OWNED BY public.iam_org_owner_history.id;

CREATE INDEX idx_iam_org_owner_history_org_time
    ON public.iam_org_owner_history (tenant_id, organization_id, changed_time DESC, id DESC);

COMMENT ON TABLE public.iam_org_owner_history IS '机构所有者初始化和转让的只追加历史';
COMMENT ON COLUMN public.iam_org_owner_history.from_account_id IS '原所有者账号；机构首次设置所有者时为空';
COMMENT ON COLUMN public.iam_org_owner_history.to_account_id IS '新所有者账号';
COMMENT ON COLUMN public.iam_org_owner_history.reason IS '敏感所有权变更的必填原因';
COMMENT ON COLUMN public.iam_org_owner_history.operator_id IS '执行操作的平台身份或租户账号 ID';

COMMIT;
