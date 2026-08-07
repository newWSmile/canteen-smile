-- script_id: IAM_DDL_0001
-- target_database: IAM
-- type: DDL
-- depends_on: NONE
-- transactional: YES
-- purpose: 创建平台身份、租户、租户编码保留、租户安全策略和全局用户名保留核心结构
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.iam_platform_identity_seq AS bigint;
CREATE SEQUENCE public.iam_tenant_seq AS bigint;
CREATE SEQUENCE public.iam_tenant_code_registry_seq AS bigint;
CREATE SEQUENCE public.iam_tenant_security_policy_seq AS bigint;
CREATE SEQUENCE public.iam_username_registry_seq AS bigint;

CREATE TABLE public.iam_platform_identity (
    id bigint NOT NULL DEFAULT nextval('public.iam_platform_identity_seq'::regclass),
    username varchar(128) NOT NULL,
    normalized_username varchar(128) NOT NULL,
    display_name varchar(128),
    status varchar(32) NOT NULL,
    authz_version bigint NOT NULL DEFAULT 0,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_platform_identity PRIMARY KEY (id),
    CONSTRAINT ck_iam_platform_identity_username_not_blank
        CHECK (btrim(username) <> '' AND btrim(normalized_username) <> ''),
    CONSTRAINT ck_iam_platform_identity_version_non_negative
        CHECK (authz_version >= 0 AND version >= 0)
);

ALTER SEQUENCE public.iam_platform_identity_seq
    OWNED BY public.iam_platform_identity.id;

CREATE UNIQUE INDEX uk_iam_platform_identity_normalized_username
    ON public.iam_platform_identity (normalized_username)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_platform_identity_status
    ON public.iam_platform_identity (status, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_platform_identity IS '独立平台身份；不属于任何租户或机构';
COMMENT ON COLUMN public.iam_platform_identity.username IS '当前展示和登录使用的原始用户名';
COMMENT ON COLUMN public.iam_platform_identity.normalized_username IS '按服务端统一规则归一化后的当前用户名';
COMMENT ON COLUMN public.iam_platform_identity.display_name IS '可选显示名称，为空时展示用户名';
COMMENT ON COLUMN public.iam_platform_identity.status IS '平台身份状态，由 IAM 应用枚举管理';
COMMENT ON COLUMN public.iam_platform_identity.authz_version IS '平台身份授权版本，变化时使旧会话失效';
COMMENT ON COLUMN public.iam_platform_identity.created_by IS '创建者平台身份 ID；系统首次引导创建时允许为空';
COMMENT ON COLUMN public.iam_platform_identity.created_time IS '创建时间';
COMMENT ON COLUMN public.iam_platform_identity.updated_by IS '最后更新者平台身份 ID';
COMMENT ON COLUMN public.iam_platform_identity.updated_time IS '最后更新时间';
COMMENT ON COLUMN public.iam_platform_identity.is_deleted IS '逻辑删除标记';
COMMENT ON COLUMN public.iam_platform_identity.version IS '乐观锁版本';

CREATE TABLE public.iam_tenant (
    id bigint NOT NULL DEFAULT nextval('public.iam_tenant_seq'::regclass),
    tenant_code varchar(64) NOT NULL,
    name varchar(200) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'INITIALIZING',
    root_organization_id bigint,
    security_version bigint NOT NULL DEFAULT 0,
    template_version bigint NOT NULL DEFAULT 0,
    provision_status varchar(32) NOT NULL DEFAULT 'INITIALIZING',
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_tenant PRIMARY KEY (id),
    CONSTRAINT uk_iam_tenant_code UNIQUE (tenant_code),
    CONSTRAINT ck_iam_tenant_code_name_not_blank
        CHECK (btrim(tenant_code) <> '' AND btrim(name) <> ''),
    CONSTRAINT ck_iam_tenant_status
        CHECK (status IN ('INITIALIZING', 'ACTIVE', 'SUSPENDED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_iam_tenant_provision_status
        CHECK (provision_status IN ('INITIALIZING', 'ACTIVE', 'PROVISION_FAILED')),
    CONSTRAINT ck_iam_tenant_version_non_negative
        CHECK (security_version >= 0 AND template_version >= 0 AND version >= 0)
);

ALTER SEQUENCE public.iam_tenant_seq
    OWNED BY public.iam_tenant.id;

CREATE INDEX idx_iam_tenant_status
    ON public.iam_tenant (status, id)
    WHERE is_deleted = false;

CREATE INDEX idx_iam_tenant_provision_status
    ON public.iam_tenant (provision_status, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_tenant IS '客户隔离边界；行政区域不参与租户隔离';
COMMENT ON COLUMN public.iam_tenant.tenant_code IS '永久唯一且原则上不可修改的租户业务标识';
COMMENT ON COLUMN public.iam_tenant.name IS '允许修改的租户名称';
COMMENT ON COLUMN public.iam_tenant.status IS '租户生命周期状态';
COMMENT ON COLUMN public.iam_tenant.root_organization_id IS '租户根机构 ID；机构表创建后追加外键';
COMMENT ON COLUMN public.iam_tenant.security_version IS '租户安全版本，提升后使租户内全部会话失效';
COMMENT ON COLUMN public.iam_tenant.template_version IS '租户初始化所使用的机构类型模板版本';
COMMENT ON COLUMN public.iam_tenant.provision_status IS '跨 Auth 初始化编排状态';
COMMENT ON COLUMN public.iam_tenant.created_by IS '创建者平台身份 ID';
COMMENT ON COLUMN public.iam_tenant.created_time IS '创建时间';
COMMENT ON COLUMN public.iam_tenant.updated_by IS '最后更新者平台身份 ID';
COMMENT ON COLUMN public.iam_tenant.updated_time IS '最后更新时间';
COMMENT ON COLUMN public.iam_tenant.is_deleted IS '逻辑删除标记；租户注销不等同于删除';
COMMENT ON COLUMN public.iam_tenant.version IS '乐观锁版本';

CREATE TABLE public.iam_tenant_code_registry (
    id bigint NOT NULL DEFAULT nextval('public.iam_tenant_code_registry_seq'::regclass),
    tenant_code varchar(64) NOT NULL,
    tenant_id bigint NOT NULL,
    reserved_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_tenant_code_registry PRIMARY KEY (id),
    CONSTRAINT uk_iam_tenant_code_registry_code UNIQUE (tenant_code),
    CONSTRAINT uk_iam_tenant_code_registry_tenant UNIQUE (tenant_id),
    CONSTRAINT ck_iam_tenant_code_registry_not_blank CHECK (btrim(tenant_code) <> ''),
    CONSTRAINT ck_iam_tenant_code_registry_never_deleted CHECK (is_deleted = false),
    CONSTRAINT ck_iam_tenant_code_registry_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_tenant_code_registry_seq
    OWNED BY public.iam_tenant_code_registry.id;

COMMENT ON TABLE public.iam_tenant_code_registry IS '永久保留所有使用过的租户业务标识，避免异常物理清理后复用';
COMMENT ON COLUMN public.iam_tenant_code_registry.tenant_code IS '永久占用的租户业务标识';
COMMENT ON COLUMN public.iam_tenant_code_registry.tenant_id IS '首次占用该标识的租户 ID，不设置外键以允许保留历史';
COMMENT ON COLUMN public.iam_tenant_code_registry.reserved_time IS '标识首次被占用的时间';
COMMENT ON COLUMN public.iam_tenant_code_registry.created_by IS '执行保留操作的平台身份 ID';
COMMENT ON COLUMN public.iam_tenant_code_registry.created_time IS '创建时间';
COMMENT ON COLUMN public.iam_tenant_code_registry.updated_by IS '最后更新者平台身份 ID，正常情况下不更新';
COMMENT ON COLUMN public.iam_tenant_code_registry.updated_time IS '最后更新时间';
COMMENT ON COLUMN public.iam_tenant_code_registry.is_deleted IS '固定为 false，永久保留记录';
COMMENT ON COLUMN public.iam_tenant_code_registry.version IS '乐观锁版本';

CREATE TABLE public.iam_tenant_security_policy (
    id bigint NOT NULL DEFAULT nextval('public.iam_tenant_security_policy_seq'::regclass),
    tenant_id bigint NOT NULL,
    concurrent_login_enabled boolean NOT NULL DEFAULT true,
    max_devices smallint NOT NULL DEFAULT 5,
    remember_me_enabled boolean NOT NULL DEFAULT true,
    idle_seconds integer NOT NULL DEFAULT 7200,
    absolute_seconds integer NOT NULL DEFAULT 604800,
    remember_idle_seconds integer NOT NULL DEFAULT 604800,
    remember_absolute_seconds integer NOT NULL DEFAULT 2592000,
    password_expiry_enabled boolean NOT NULL DEFAULT false,
    password_expiry_days integer,
    audit_retention_days integer NOT NULL DEFAULT 180,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_tenant_security_policy PRIMARY KEY (id),
    CONSTRAINT uk_iam_tenant_security_policy_tenant UNIQUE (tenant_id),
    CONSTRAINT fk_iam_tenant_security_policy_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_tenant_security_policy_max_devices CHECK (max_devices >= 1),
    CONSTRAINT ck_iam_tenant_security_policy_normal_session
        CHECK (idle_seconds > 0 AND absolute_seconds > 0 AND idle_seconds <= absolute_seconds),
    CONSTRAINT ck_iam_tenant_security_policy_remember_session
        CHECK (
            remember_idle_seconds > 0
            AND remember_absolute_seconds > 0
            AND remember_idle_seconds <= remember_absolute_seconds
        ),
    CONSTRAINT ck_iam_tenant_security_policy_password_expiry
        CHECK (
            (password_expiry_days IS NULL OR password_expiry_days > 0)
            AND (password_expiry_enabled = false OR password_expiry_days IS NOT NULL)
        ),
    CONSTRAINT ck_iam_tenant_security_policy_audit_retention
        CHECK (audit_retention_days >= 180),
    CONSTRAINT ck_iam_tenant_security_policy_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_tenant_security_policy_seq
    OWNED BY public.iam_tenant_security_policy.id;

COMMENT ON TABLE public.iam_tenant_security_policy IS '租户可调整的登录、设备、记住我、密码到期和审计保留策略';
COMMENT ON COLUMN public.iam_tenant_security_policy.tenant_id IS '策略所属租户 ID';
COMMENT ON COLUMN public.iam_tenant_security_policy.concurrent_login_enabled IS '是否允许同一账号多设备并发登录';
COMMENT ON COLUMN public.iam_tenant_security_policy.max_devices IS '允许并发时的最大有效设备数，默认 5';
COMMENT ON COLUMN public.iam_tenant_security_policy.remember_me_enabled IS '租户是否允许用户使用记住我';
COMMENT ON COLUMN public.iam_tenant_security_policy.idle_seconds IS '普通会话空闲超时秒数，默认 2 小时';
COMMENT ON COLUMN public.iam_tenant_security_policy.absolute_seconds IS '普通会话最长存活秒数，默认 7 天';
COMMENT ON COLUMN public.iam_tenant_security_policy.remember_idle_seconds IS '记住我会话空闲超时秒数，默认 7 天';
COMMENT ON COLUMN public.iam_tenant_security_policy.remember_absolute_seconds IS '记住我会话最长存活秒数，默认 30 天';
COMMENT ON COLUMN public.iam_tenant_security_policy.password_expiry_enabled IS '是否启用租户密码定期到期';
COMMENT ON COLUMN public.iam_tenant_security_policy.password_expiry_days IS '启用密码到期后的有效天数';
COMMENT ON COLUMN public.iam_tenant_security_policy.audit_retention_days IS '租户审计保留天数，最低 180 天';
COMMENT ON COLUMN public.iam_tenant_security_policy.created_by IS '创建策略的平台或租户身份 ID';
COMMENT ON COLUMN public.iam_tenant_security_policy.created_time IS '创建时间';
COMMENT ON COLUMN public.iam_tenant_security_policy.updated_by IS '最后更新者身份 ID';
COMMENT ON COLUMN public.iam_tenant_security_policy.updated_time IS '最后更新时间';
COMMENT ON COLUMN public.iam_tenant_security_policy.is_deleted IS '逻辑删除标记';
COMMENT ON COLUMN public.iam_tenant_security_policy.version IS '乐观锁版本';

CREATE TABLE public.iam_username_registry (
    id bigint NOT NULL DEFAULT nextval('public.iam_username_registry_seq'::regclass),
    normalized_username varchar(128) NOT NULL,
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    original_username varchar(128) NOT NULL,
    login_enabled boolean NOT NULL DEFAULT true,
    reserved_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disabled_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_username_registry PRIMARY KEY (id),
    CONSTRAINT uk_iam_username_registry_username UNIQUE (normalized_username),
    CONSTRAINT ck_iam_username_registry_subject_type
        CHECK (subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_iam_username_registry_username_not_blank
        CHECK (btrim(normalized_username) <> '' AND btrim(original_username) <> ''),
    CONSTRAINT ck_iam_username_registry_subject_id_positive CHECK (subject_id > 0),
    CONSTRAINT ck_iam_username_registry_login_state
        CHECK (
            (login_enabled = true AND disabled_time IS NULL)
            OR (login_enabled = false AND disabled_time IS NOT NULL)
        ),
    CONSTRAINT ck_iam_username_registry_never_deleted CHECK (is_deleted = false),
    CONSTRAINT ck_iam_username_registry_version_non_negative CHECK (version >= 0)
);

ALTER SEQUENCE public.iam_username_registry_seq
    OWNED BY public.iam_username_registry.id;

CREATE UNIQUE INDEX uk_iam_username_registry_active_subject
    ON public.iam_username_registry (subject_type, subject_id)
    WHERE login_enabled = true AND is_deleted = false;

CREATE INDEX idx_iam_username_registry_subject_history
    ON public.iam_username_registry (subject_type, subject_id, reserved_time DESC);

COMMENT ON TABLE public.iam_username_registry IS '平台和租户账号共用的全局用户名永久保留表';
COMMENT ON COLUMN public.iam_username_registry.normalized_username IS '全局唯一、不区分大小写规则下的归一化用户名';
COMMENT ON COLUMN public.iam_username_registry.subject_type IS '用户名所属身份类型：平台身份或租户账号';
COMMENT ON COLUMN public.iam_username_registry.subject_id IS '身份 ID；多态引用不设置数据库外键';
COMMENT ON COLUMN public.iam_username_registry.original_username IS '用户提交并保留审计含义的原始用户名';
COMMENT ON COLUMN public.iam_username_registry.login_enabled IS '是否为该身份当前允许登录的用户名';
COMMENT ON COLUMN public.iam_username_registry.reserved_time IS '用户名首次永久占用时间';
COMMENT ON COLUMN public.iam_username_registry.disabled_time IS '旧用户名停止登录的时间';
COMMENT ON COLUMN public.iam_username_registry.created_by IS '执行用户名保留操作的身份 ID';
COMMENT ON COLUMN public.iam_username_registry.created_time IS '创建时间';
COMMENT ON COLUMN public.iam_username_registry.updated_by IS '最后执行用户名切换的身份 ID';
COMMENT ON COLUMN public.iam_username_registry.updated_time IS '最后更新时间';
COMMENT ON COLUMN public.iam_username_registry.is_deleted IS '固定为 false，所有曾用用户名永久保留';
COMMENT ON COLUMN public.iam_username_registry.version IS '乐观锁版本';

COMMIT;
