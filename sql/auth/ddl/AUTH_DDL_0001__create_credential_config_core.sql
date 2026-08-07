-- script_id: AUTH_DDL_0001
-- target_database: AUTH
-- type: DDL
-- depends_on: NONE
-- transactional: YES
-- purpose: 创建认证凭证、密码历史、手机号绑定、登录失败及平台管理的短信和安全链接配置结构
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.auth_credential_seq AS bigint;
CREATE SEQUENCE public.auth_password_history_seq AS bigint;
CREATE SEQUENCE public.auth_mobile_binding_seq AS bigint;
CREATE SEQUENCE public.auth_login_failure_seq AS bigint;
CREATE SEQUENCE public.auth_sms_provider_config_seq AS bigint;
CREATE SEQUENCE public.auth_security_link_config_seq AS bigint;
CREATE SEQUENCE public.auth_sms_template_config_seq AS bigint;

CREATE TABLE public.auth_credential (
    id bigint NOT NULL DEFAULT nextval('public.auth_credential_seq'::regclass),
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    password_hash varchar(512),
    algorithm varchar(64),
    password_changed_at timestamptz,
    credential_version bigint NOT NULL DEFAULT 0,
    status varchar(32) NOT NULL DEFAULT 'PENDING',
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_credential PRIMARY KEY (id),
    CONSTRAINT uk_auth_credential_subject UNIQUE (subject_type, subject_id),
    CONSTRAINT ck_auth_credential_subject_type
        CHECK (subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_auth_credential_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'RESET_REQUIRED', 'DISABLED')),
    CONSTRAINT ck_auth_credential_secret_state
        CHECK (
            (
                status = 'PENDING'
                AND password_hash IS NULL
                AND algorithm IS NULL
                AND password_changed_at IS NULL
            )
            OR (
                status <> 'PENDING'
                AND password_hash IS NOT NULL
                AND btrim(password_hash) <> ''
                AND algorithm IS NOT NULL
                AND btrim(algorithm) <> ''
                AND password_changed_at IS NOT NULL
            )
        ),
    CONSTRAINT ck_auth_credential_version_non_negative
        CHECK (subject_id > 0 AND credential_version >= 0 AND version >= 0)
);

ALTER SEQUENCE public.auth_credential_seq OWNED BY public.auth_credential.id;

CREATE INDEX idx_auth_credential_status
    ON public.auth_credential (status, subject_type, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.auth_credential IS '平台身份和租户账号的密码凭证；不保存 IAM 资料';
COMMENT ON COLUMN public.auth_credential.password_hash IS '安全评审确认算法后的自适应强哈希，禁止明文或可逆密码';

CREATE TABLE public.auth_password_history (
    id bigint NOT NULL DEFAULT nextval('public.auth_password_history_seq'::regclass),
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    password_hash varchar(512) NOT NULL,
    algorithm varchar(64) NOT NULL,
    changed_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_password_history PRIMARY KEY (id),
    CONSTRAINT ck_auth_password_history_subject_type
        CHECK (subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_auth_password_history_value
        CHECK (
            subject_id > 0
            AND btrim(password_hash) <> ''
            AND btrim(algorithm) <> ''
            AND is_deleted = false
            AND version >= 0
        )
);

ALTER SEQUENCE public.auth_password_history_seq OWNED BY public.auth_password_history.id;

CREATE INDEX idx_auth_password_history_subject_time
    ON public.auth_password_history (subject_type, subject_id, changed_time DESC, id DESC);

COMMENT ON TABLE public.auth_password_history IS '密码历史摘要，用于最近五次不可复用检查和安全审计';

CREATE TABLE public.auth_mobile_binding (
    id bigint NOT NULL DEFAULT nextval('public.auth_mobile_binding_seq'::regclass),
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    mobile_ciphertext bytea NOT NULL,
    mobile_hash varchar(64) NOT NULL,
    masked_mobile varchar(32) NOT NULL,
    encryption_key_id varchar(128) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'PENDING',
    verified_time timestamptz,
    replaced_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_mobile_binding PRIMARY KEY (id),
    CONSTRAINT ck_auth_mobile_binding_subject_type
        CHECK (subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_auth_mobile_binding_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REPLACED', 'REVOKED')),
    CONSTRAINT ck_auth_mobile_binding_value
        CHECK (
            subject_id > 0
            AND octet_length(mobile_ciphertext) > 0
            AND mobile_hash ~ '^[0-9a-fA-F]{64}$'
            AND btrim(masked_mobile) <> ''
            AND btrim(encryption_key_id) <> ''
            AND version >= 0
        ),
    CONSTRAINT ck_auth_mobile_binding_verified_state
        CHECK (status <> 'VERIFIED' OR verified_time IS NOT NULL),
    CONSTRAINT ck_auth_mobile_binding_replaced_state
        CHECK (status <> 'REPLACED' OR replaced_time IS NOT NULL)
);

ALTER SEQUENCE public.auth_mobile_binding_seq OWNED BY public.auth_mobile_binding.id;

CREATE UNIQUE INDEX uk_auth_mobile_binding_verified_subject
    ON public.auth_mobile_binding (subject_type, subject_id)
    WHERE status = 'VERIFIED' AND is_deleted = false;

CREATE INDEX idx_auth_mobile_binding_mobile_hash
    ON public.auth_mobile_binding (mobile_hash, subject_type, subject_id)
    WHERE status = 'VERIFIED' AND is_deleted = false;

COMMENT ON TABLE public.auth_mobile_binding IS '账号手机号绑定；同一手机号摘要允许对应多个账号';
COMMENT ON COLUMN public.auth_mobile_binding.mobile_ciphertext IS '手机号加密密文，根加密密钥只能来自环境变量或密钥服务';
COMMENT ON COLUMN public.auth_mobile_binding.mobile_hash IS '带服务端 Pepper 的 HMAC 查询摘要，不是普通无盐哈希';

CREATE TABLE public.auth_login_failure (
    id bigint NOT NULL DEFAULT nextval('public.auth_login_failure_seq'::regclass),
    subject_key_hash varchar(64) NOT NULL,
    last_ip_hash varchar(64),
    last_device_hash varchar(64),
    password_failures integer NOT NULL DEFAULT 0,
    captcha_required boolean NOT NULL DEFAULT false,
    locked_until timestamptz,
    last_failure_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_login_failure PRIMARY KEY (id),
    CONSTRAINT uk_auth_login_failure_subject UNIQUE (subject_key_hash),
    CONSTRAINT ck_auth_login_failure_value
        CHECK (
            subject_key_hash ~ '^[0-9a-fA-F]{64}$'
            AND (last_ip_hash IS NULL OR last_ip_hash ~ '^[0-9a-fA-F]{64}$')
            AND (last_device_hash IS NULL OR last_device_hash ~ '^[0-9a-fA-F]{64}$')
            AND password_failures >= 0
            AND version >= 0
        ),
    CONSTRAINT ck_auth_login_failure_captcha
        CHECK (password_failures < 3 OR captcha_required = true),
    CONSTRAINT ck_auth_login_failure_lock
        CHECK (password_failures < 5 OR locked_until IS NOT NULL)
);

ALTER SEQUENCE public.auth_login_failure_seq OWNED BY public.auth_login_failure.id;

CREATE INDEX idx_auth_login_failure_lock_expiration
    ON public.auth_login_failure (locked_until, id)
    WHERE locked_until IS NOT NULL AND is_deleted = false;

COMMENT ON TABLE public.auth_login_failure IS '密码连续失败、验证码门槛和临时锁定；IP/设备频率限制主要由 Redis 承担';

CREATE TABLE public.auth_sms_provider_config (
    id bigint NOT NULL DEFAULT nextval('public.auth_sms_provider_config_seq'::regclass),
    config_name varchar(128) NOT NULL,
    provider_code varchar(64) NOT NULL,
    endpoint_url varchar(512),
    credential_ciphertext bytea NOT NULL,
    encryption_key_id varchar(128) NOT NULL,
    credential_fingerprint varchar(128) NOT NULL,
    properties_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(32) NOT NULL DEFAULT 'DISABLED',
    is_default boolean NOT NULL DEFAULT false,
    last_verified_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_sms_provider_config PRIMARY KEY (id),
    CONSTRAINT uk_auth_sms_provider_config_name UNIQUE (config_name),
    CONSTRAINT ck_auth_sms_provider_config_status CHECK (status IN ('DISABLED', 'ENABLED')),
    CONSTRAINT ck_auth_sms_provider_config_value
        CHECK (
            btrim(config_name) <> ''
            AND btrim(provider_code) <> ''
            AND (endpoint_url IS NULL OR btrim(endpoint_url) <> '')
            AND octet_length(credential_ciphertext) > 0
            AND btrim(encryption_key_id) <> ''
            AND btrim(credential_fingerprint) <> ''
            AND jsonb_typeof(properties_json) = 'object'
            AND version >= 0
        )
);

ALTER SEQUENCE public.auth_sms_provider_config_seq OWNED BY public.auth_sms_provider_config.id;

CREATE UNIQUE INDEX uk_auth_sms_provider_config_default
    ON public.auth_sms_provider_config (is_default)
    WHERE is_default = true AND status = 'ENABLED' AND is_deleted = false;

COMMENT ON TABLE public.auth_sms_provider_config IS '平台超级管理员维护的短信供应商配置；不通过 DML 初始化真实值';
COMMENT ON COLUMN public.auth_sms_provider_config.credential_ciphertext IS '供应商访问密钥的整体加密密文，接口永不返回明文';
COMMENT ON COLUMN public.auth_sms_provider_config.properties_json IS '供应商非敏感扩展参数，敏感字段必须进入加密密文';

CREATE TABLE public.auth_security_link_config (
    id bigint NOT NULL DEFAULT nextval('public.auth_security_link_config_seq'::regclass),
    app_code varchar(32) NOT NULL,
    activation_base_url varchar(512) NOT NULL,
    password_reset_base_url varchar(512) NOT NULL,
    allowed_hosts jsonb NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'DISABLED',
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_security_link_config PRIMARY KEY (id),
    CONSTRAINT uk_auth_security_link_config_app UNIQUE (app_code),
    CONSTRAINT ck_auth_security_link_config_app
        CHECK (app_code IN ('PLATFORM_ADMIN', 'TENANT_ADMIN', 'TENANT_PORTAL')),
    CONSTRAINT ck_auth_security_link_config_status CHECK (status IN ('DISABLED', 'ENABLED')),
    CONSTRAINT ck_auth_security_link_config_value
        CHECK (
            btrim(activation_base_url) <> ''
            AND btrim(password_reset_base_url) <> ''
            AND jsonb_typeof(allowed_hosts) = 'array'
            AND jsonb_array_length(allowed_hosts) > 0
            AND version >= 0
        )
);

ALTER SEQUENCE public.auth_security_link_config_seq OWNED BY public.auth_security_link_config.id;

COMMENT ON TABLE public.auth_security_link_config IS '平台超级管理员维护的激活和密码重置前端地址白名单';
COMMENT ON COLUMN public.auth_security_link_config.allowed_hosts IS '防止一次性票据被拼接到未授权域名的主机白名单';

CREATE TABLE public.auth_sms_template_config (
    id bigint NOT NULL DEFAULT nextval('public.auth_sms_template_config_seq'::regclass),
    provider_config_id bigint NOT NULL,
    purpose varchar(64) NOT NULL,
    template_code varchar(128) NOT NULL,
    signature_name varchar(128),
    parameter_schema jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(32) NOT NULL DEFAULT 'DISABLED',
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_sms_template_config PRIMARY KEY (id),
    CONSTRAINT uk_auth_sms_template_config_purpose UNIQUE (provider_config_id, purpose),
    CONSTRAINT fk_auth_sms_template_config_provider
        FOREIGN KEY (provider_config_id) REFERENCES public.auth_sms_provider_config (id) ON DELETE RESTRICT,
    CONSTRAINT ck_auth_sms_template_config_purpose
        CHECK (
            purpose IN (
                'LOGIN', 'ACTIVATION', 'PASSWORD_RESET', 'MOBILE_BIND',
                'MOBILE_CHANGE', 'ADMIN_REAUTH', 'PLATFORM_SECOND_FACTOR'
            )
        ),
    CONSTRAINT ck_auth_sms_template_config_status CHECK (status IN ('DISABLED', 'ENABLED')),
    CONSTRAINT ck_auth_sms_template_config_value
        CHECK (
            btrim(template_code) <> ''
            AND (signature_name IS NULL OR btrim(signature_name) <> '')
            AND jsonb_typeof(parameter_schema) = 'object'
            AND version >= 0
        )
);

ALTER SEQUENCE public.auth_sms_template_config_seq OWNED BY public.auth_sms_template_config.id;

CREATE INDEX idx_auth_sms_template_config_status
    ON public.auth_sms_template_config (provider_config_id, status, purpose)
    WHERE is_deleted = false;

COMMENT ON TABLE public.auth_sms_template_config IS '平台超级管理员维护的短信用途与供应商模板映射；无初始化秘密数据';

COMMIT;
