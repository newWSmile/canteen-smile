-- script_id: AUTH_DDL_0002
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0001
-- transactional: YES
-- purpose: 创建短信挑战、账号选择、激活、密码重置、平台恢复码和敏感操作再认证票据结构
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.auth_sms_challenge_seq AS bigint;
CREATE SEQUENCE public.auth_account_selector_ticket_seq AS bigint;
CREATE SEQUENCE public.auth_activation_ticket_seq AS bigint;
CREATE SEQUENCE public.auth_password_reset_ticket_seq AS bigint;
CREATE SEQUENCE public.auth_platform_recovery_code_seq AS bigint;
CREATE SEQUENCE public.auth_reauth_ticket_seq AS bigint;

CREATE TABLE public.auth_sms_challenge (
    id bigint NOT NULL DEFAULT nextval('public.auth_sms_challenge_seq'::regclass),
    challenge_id varchar(64) NOT NULL,
    purpose varchar(64) NOT NULL,
    mobile_hash varchar(64) NOT NULL,
    code_hash varchar(128) NOT NULL,
    provider_config_id bigint NOT NULL,
    template_config_id bigint NOT NULL,
    attempts smallint NOT NULL DEFAULT 0,
    status varchar(32) NOT NULL DEFAULT 'PENDING',
    sent_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL,
    consumed_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_sms_challenge PRIMARY KEY (id),
    CONSTRAINT uk_auth_sms_challenge_id UNIQUE (challenge_id),
    CONSTRAINT fk_auth_sms_challenge_provider
        FOREIGN KEY (provider_config_id) REFERENCES public.auth_sms_provider_config (id) ON DELETE RESTRICT,
    CONSTRAINT fk_auth_sms_challenge_template
        FOREIGN KEY (template_config_id) REFERENCES public.auth_sms_template_config (id) ON DELETE RESTRICT,
    CONSTRAINT ck_auth_sms_challenge_purpose
        CHECK (
            purpose IN (
                'LOGIN', 'ACTIVATION', 'PASSWORD_RESET', 'MOBILE_BIND',
                'MOBILE_CHANGE', 'ADMIN_REAUTH', 'PLATFORM_SECOND_FACTOR'
            )
        ),
    CONSTRAINT ck_auth_sms_challenge_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'CONSUMED', 'EXPIRED', 'INVALIDATED')),
    CONSTRAINT ck_auth_sms_challenge_value
        CHECK (
            btrim(challenge_id) <> ''
            AND mobile_hash ~ '^[0-9a-fA-F]{64}$'
            AND btrim(code_hash) <> ''
            AND attempts BETWEEN 0 AND 5
            AND expires_at > sent_time
            AND version >= 0
        ),
    CONSTRAINT ck_auth_sms_challenge_consumed_state
        CHECK (status <> 'CONSUMED' OR consumed_time IS NOT NULL)
);

ALTER SEQUENCE public.auth_sms_challenge_seq OWNED BY public.auth_sms_challenge.id;

CREATE INDEX idx_auth_sms_challenge_mobile_time
    ON public.auth_sms_challenge (mobile_hash, purpose, sent_time DESC, id DESC);

CREATE INDEX idx_auth_sms_challenge_expiration
    ON public.auth_sms_challenge (expires_at, id)
    WHERE status IN ('PENDING', 'VERIFIED') AND is_deleted = false;

COMMENT ON TABLE public.auth_sms_challenge IS '短信验证码挑战；验证码只保存摘要，错误五次后失效';

CREATE TABLE public.auth_account_selector_ticket (
    id bigint NOT NULL DEFAULT nextval('public.auth_account_selector_ticket_seq'::regclass),
    ticket_hash varchar(64) NOT NULL,
    mobile_hash varchar(64) NOT NULL,
    candidate_digest varchar(64) NOT NULL,
    app_code varchar(32) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at timestamptz NOT NULL,
    consumed_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_account_selector_ticket PRIMARY KEY (id),
    CONSTRAINT uk_auth_account_selector_ticket_hash UNIQUE (ticket_hash),
    CONSTRAINT ck_auth_account_selector_ticket_app
        CHECK (app_code IN ('TENANT_ADMIN', 'TENANT_PORTAL')),
    CONSTRAINT ck_auth_account_selector_ticket_status
        CHECK (status IN ('ACTIVE', 'CONSUMED', 'EXPIRED', 'INVALIDATED')),
    CONSTRAINT ck_auth_account_selector_ticket_value
        CHECK (
            ticket_hash ~ '^[0-9a-fA-F]{64}$'
            AND mobile_hash ~ '^[0-9a-fA-F]{64}$'
            AND candidate_digest ~ '^[0-9a-fA-F]{64}$'
            AND expires_at > created_time
            AND version >= 0
        ),
    CONSTRAINT ck_auth_account_selector_ticket_consumed
        CHECK (status <> 'CONSUMED' OR consumed_time IS NOT NULL)
);

ALTER SEQUENCE public.auth_account_selector_ticket_seq OWNED BY public.auth_account_selector_ticket.id;

CREATE INDEX idx_auth_account_selector_ticket_expiration
    ON public.auth_account_selector_ticket (expires_at, id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

COMMENT ON TABLE public.auth_account_selector_ticket IS '手机号验证成功后用于选择具体租户账号的短期一次性票据';

CREATE TABLE public.auth_activation_ticket (
    id bigint NOT NULL DEFAULT nextval('public.auth_activation_ticket_seq'::regclass),
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    ticket_hash varchar(64) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at timestamptz NOT NULL,
    consumed_time timestamptz,
    superseded_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_activation_ticket PRIMARY KEY (id),
    CONSTRAINT uk_auth_activation_ticket_hash UNIQUE (ticket_hash),
    CONSTRAINT ck_auth_activation_ticket_subject_type
        CHECK (subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_auth_activation_ticket_status
        CHECK (status IN ('ACTIVE', 'CONSUMED', 'EXPIRED', 'SUPERSEDED')),
    CONSTRAINT ck_auth_activation_ticket_value
        CHECK (
            subject_id > 0
            AND ticket_hash ~ '^[0-9a-fA-F]{64}$'
            AND expires_at > created_time
            AND version >= 0
        ),
    CONSTRAINT ck_auth_activation_ticket_consumed
        CHECK (status <> 'CONSUMED' OR consumed_time IS NOT NULL),
    CONSTRAINT ck_auth_activation_ticket_superseded
        CHECK (status <> 'SUPERSEDED' OR superseded_time IS NOT NULL)
);

ALTER SEQUENCE public.auth_activation_ticket_seq OWNED BY public.auth_activation_ticket.id;

CREATE UNIQUE INDEX uk_auth_activation_ticket_active_subject
    ON public.auth_activation_ticket (subject_type, subject_id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

CREATE INDEX idx_auth_activation_ticket_expiration
    ON public.auth_activation_ticket (expires_at, id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

COMMENT ON TABLE public.auth_activation_ticket IS '默认二十四小时有效的一次性账号激活链接票据摘要';

CREATE TABLE public.auth_password_reset_ticket (
    id bigint NOT NULL DEFAULT nextval('public.auth_password_reset_ticket_seq'::regclass),
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    reset_mode varchar(32) NOT NULL,
    ticket_hash varchar(64) NOT NULL,
    initiated_by_type varchar(32) NOT NULL,
    initiated_by_id bigint NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at timestamptz NOT NULL,
    consumed_time timestamptz,
    superseded_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_password_reset_ticket PRIMARY KEY (id),
    CONSTRAINT uk_auth_password_reset_ticket_hash UNIQUE (ticket_hash),
    CONSTRAINT ck_auth_password_reset_ticket_subject_type
        CHECK (subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_auth_password_reset_ticket_mode CHECK (reset_mode IN ('SMS', 'ONE_TIME_LINK', 'RECOVERY_CODE')),
    CONSTRAINT ck_auth_password_reset_ticket_initiator
        CHECK (initiated_by_type IN ('SELF', 'PLATFORM_IDENTITY', 'TENANT_ACCOUNT', 'SYSTEM')),
    CONSTRAINT ck_auth_password_reset_ticket_status
        CHECK (status IN ('ACTIVE', 'CONSUMED', 'EXPIRED', 'SUPERSEDED')),
    CONSTRAINT ck_auth_password_reset_ticket_value
        CHECK (
            subject_id > 0
            AND initiated_by_id >= 0
            AND ticket_hash ~ '^[0-9a-fA-F]{64}$'
            AND expires_at > created_time
            AND version >= 0
        ),
    CONSTRAINT ck_auth_password_reset_ticket_consumed
        CHECK (status <> 'CONSUMED' OR consumed_time IS NOT NULL),
    CONSTRAINT ck_auth_password_reset_ticket_superseded
        CHECK (status <> 'SUPERSEDED' OR superseded_time IS NOT NULL)
);

ALTER SEQUENCE public.auth_password_reset_ticket_seq OWNED BY public.auth_password_reset_ticket.id;

CREATE UNIQUE INDEX uk_auth_password_reset_ticket_active_subject
    ON public.auth_password_reset_ticket (subject_type, subject_id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

CREATE INDEX idx_auth_password_reset_ticket_expiration
    ON public.auth_password_reset_ticket (expires_at, id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

COMMENT ON TABLE public.auth_password_reset_ticket IS '自助、管理员或平台恢复流程使用的一次性改密票据摘要';

CREATE TABLE public.auth_platform_recovery_code (
    id bigint NOT NULL DEFAULT nextval('public.auth_platform_recovery_code_seq'::regclass),
    platform_identity_id bigint NOT NULL,
    batch_id varchar(64) NOT NULL,
    code_hash varchar(128) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    consumed_time timestamptz,
    superseded_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_platform_recovery_code PRIMARY KEY (id),
    CONSTRAINT uk_auth_platform_recovery_code_hash UNIQUE (code_hash),
    CONSTRAINT ck_auth_platform_recovery_code_status
        CHECK (status IN ('ACTIVE', 'CONSUMED', 'SUPERSEDED')),
    CONSTRAINT ck_auth_platform_recovery_code_value
        CHECK (
            platform_identity_id > 0
            AND btrim(batch_id) <> ''
            AND btrim(code_hash) <> ''
            AND version >= 0
        ),
    CONSTRAINT ck_auth_platform_recovery_code_consumed
        CHECK (status <> 'CONSUMED' OR consumed_time IS NOT NULL),
    CONSTRAINT ck_auth_platform_recovery_code_superseded
        CHECK (status <> 'SUPERSEDED' OR superseded_time IS NOT NULL)
);

ALTER SEQUENCE public.auth_platform_recovery_code_seq OWNED BY public.auth_platform_recovery_code.id;

CREATE INDEX idx_auth_platform_recovery_code_active
    ON public.auth_platform_recovery_code (platform_identity_id, batch_id, id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

COMMENT ON TABLE public.auth_platform_recovery_code IS '平台身份一次性恢复码摘要；重新生成批次后旧批次全部失效';

CREATE TABLE public.auth_reauth_ticket (
    id bigint NOT NULL DEFAULT nextval('public.auth_reauth_ticket_seq'::regclass),
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    ticket_hash varchar(64) NOT NULL,
    allowed_action varchar(128) NOT NULL,
    verify_method varchar(32) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at timestamptz NOT NULL,
    consumed_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_reauth_ticket PRIMARY KEY (id),
    CONSTRAINT uk_auth_reauth_ticket_hash UNIQUE (ticket_hash),
    CONSTRAINT ck_auth_reauth_ticket_subject_type
        CHECK (subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_auth_reauth_ticket_verify_method CHECK (verify_method IN ('PASSWORD', 'SMS')),
    CONSTRAINT ck_auth_reauth_ticket_status
        CHECK (status IN ('ACTIVE', 'CONSUMED', 'EXPIRED', 'INVALIDATED')),
    CONSTRAINT ck_auth_reauth_ticket_value
        CHECK (
            subject_id > 0
            AND ticket_hash ~ '^[0-9a-fA-F]{64}$'
            AND btrim(allowed_action) <> ''
            AND expires_at > created_time
            AND version >= 0
        ),
    CONSTRAINT ck_auth_reauth_ticket_consumed
        CHECK (status <> 'CONSUMED' OR consumed_time IS NOT NULL)
);

ALTER SEQUENCE public.auth_reauth_ticket_seq OWNED BY public.auth_reauth_ticket.id;

CREATE INDEX idx_auth_reauth_ticket_active_subject
    ON public.auth_reauth_ticket (subject_type, subject_id, allowed_action, expires_at, id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

COMMENT ON TABLE public.auth_reauth_ticket IS '敏感管理操作的五分钟一次性再认证票据摘要';

COMMIT;
