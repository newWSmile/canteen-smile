-- script_id: AUTH_DDL_0003
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0002
-- transactional: YES
-- purpose: 创建设备会话、权限快照、内部事件幂等、Auth 安全审计和外部命令幂等结构
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.auth_device_session_seq AS bigint;
CREATE SEQUENCE public.auth_permission_snapshot_seq AS bigint;
CREATE SEQUENCE public.auth_consumed_event_seq AS bigint;
CREATE SEQUENCE public.auth_audit_log_seq AS bigint;
CREATE SEQUENCE public.auth_idempotency_record_seq AS bigint;

CREATE TABLE public.auth_device_session (
    id bigint NOT NULL DEFAULT nextval('public.auth_device_session_seq'::regclass),
    session_id varchar(64) NOT NULL,
    subject_type varchar(32) NOT NULL,
    subject_id bigint NOT NULL,
    tenant_id bigint,
    organization_id bigint,
    app_code varchar(32) NOT NULL,
    token_digest varchar(128) NOT NULL,
    device_id_hash varchar(64) NOT NULL,
    device_type varchar(64) NOT NULL,
    device_name varchar(128) NOT NULL,
    login_method varchar(32) NOT NULL,
    login_ip_masked varchar(128),
    login_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idle_expires_at timestamptz NOT NULL,
    absolute_expires_at timestamptz NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    snapshot_version bigint NOT NULL DEFAULT 0,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_device_session PRIMARY KEY (id),
    CONSTRAINT uk_auth_device_session_id UNIQUE (session_id),
    CONSTRAINT ck_auth_device_session_subject_type
        CHECK (subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_auth_device_session_app
        CHECK (app_code IN ('PLATFORM_ADMIN', 'TENANT_ADMIN', 'TENANT_PORTAL')),
    CONSTRAINT ck_auth_device_session_identity_context
        CHECK (
            (
                subject_type = 'PLATFORM_IDENTITY'
                AND app_code = 'PLATFORM_ADMIN'
                AND tenant_id IS NULL
                AND organization_id IS NULL
            )
            OR (
                subject_type = 'TENANT_ACCOUNT'
                AND app_code IN ('TENANT_ADMIN', 'TENANT_PORTAL')
                AND tenant_id IS NOT NULL
                AND organization_id IS NOT NULL
            )
        ),
    CONSTRAINT ck_auth_device_session_login_method
        CHECK (login_method IN ('PASSWORD', 'SMS', 'PASSWORD_SMS', 'RECOVERY_CODE')),
    CONSTRAINT ck_auth_device_session_status
        CHECK (status IN ('ACTIVE', 'LOGGED_OUT', 'INVALIDATED', 'EXPIRED')),
    CONSTRAINT ck_auth_device_session_value
        CHECK (
            subject_id > 0
            AND btrim(session_id) <> ''
            AND btrim(token_digest) <> ''
            AND device_id_hash ~ '^[0-9a-fA-F]{64}$'
            AND btrim(device_type) <> ''
            AND btrim(device_name) <> ''
            AND last_active_time >= login_time
            AND idle_expires_at > login_time
            AND absolute_expires_at > login_time
            AND idle_expires_at <= absolute_expires_at
            AND snapshot_version >= 0
            AND version >= 0
        )
);

ALTER SEQUENCE public.auth_device_session_seq OWNED BY public.auth_device_session.id;

CREATE INDEX idx_auth_device_session_subject_status
    ON public.auth_device_session (subject_type, subject_id, status, last_active_time DESC, id DESC);

CREATE INDEX idx_auth_device_session_tenant_status
    ON public.auth_device_session (tenant_id, status, id)
    WHERE tenant_id IS NOT NULL;

CREATE INDEX idx_auth_device_session_expiration
    ON public.auth_device_session (absolute_expires_at, id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

COMMENT ON TABLE public.auth_device_session IS '账号每台设备的独立会话索引；Sa-Token Redis 状态不得替代本表审计';
COMMENT ON COLUMN public.auth_device_session.token_digest IS 'Token 摘要，禁止保存或日志输出 Token 明文';

CREATE TABLE public.auth_permission_snapshot (
    id bigint NOT NULL DEFAULT nextval('public.auth_permission_snapshot_seq'::regclass),
    session_id varchar(64) NOT NULL,
    snapshot_version bigint NOT NULL,
    payload_json jsonb NOT NULL,
    signature varchar(128) NOT NULL,
    signature_key_id varchar(128) NOT NULL,
    user_authz_version bigint NOT NULL,
    role_version_digest varchar(64) NOT NULL,
    org_path_version bigint NOT NULL,
    tenant_security_version bigint NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at timestamptz NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_permission_snapshot PRIMARY KEY (id),
    CONSTRAINT uk_auth_permission_snapshot_version UNIQUE (session_id, snapshot_version),
    CONSTRAINT fk_auth_permission_snapshot_session
        FOREIGN KEY (session_id) REFERENCES public.auth_device_session (session_id) ON DELETE RESTRICT,
    CONSTRAINT ck_auth_permission_snapshot_status
        CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'EXPIRED', 'INVALIDATED')),
    CONSTRAINT ck_auth_permission_snapshot_value
        CHECK (
            snapshot_version >= 0
            AND jsonb_typeof(payload_json) = 'object'
            AND btrim(signature) <> ''
            AND btrim(signature_key_id) <> ''
            AND role_version_digest ~ '^[0-9a-fA-F]{64}$'
            AND user_authz_version >= 0
            AND org_path_version >= 0
            AND tenant_security_version >= 0
            AND expires_at > created_time
            AND version >= 0
        )
);

ALTER SEQUENCE public.auth_permission_snapshot_seq OWNED BY public.auth_permission_snapshot.id;

CREATE UNIQUE INDEX uk_auth_permission_snapshot_active_session
    ON public.auth_permission_snapshot (session_id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

CREATE INDEX idx_auth_permission_snapshot_expiration
    ON public.auth_permission_snapshot (expires_at, id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

COMMENT ON TABLE public.auth_permission_snapshot IS 'IAM 签发并验证版本的会话权限快照；缺失或签名失败必须拒绝访问';

CREATE TABLE public.auth_consumed_event (
    id bigint NOT NULL DEFAULT nextval('public.auth_consumed_event_seq'::regclass),
    event_id varchar(64) NOT NULL,
    event_type varchar(128) NOT NULL,
    payload_digest varchar(64) NOT NULL,
    result varchar(32) NOT NULL,
    consumed_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_consumed_event PRIMARY KEY (id),
    CONSTRAINT uk_auth_consumed_event_id UNIQUE (event_id),
    CONSTRAINT ck_auth_consumed_event_result CHECK (result IN ('SUCCESS', 'IGNORED', 'FAILED')),
    CONSTRAINT ck_auth_consumed_event_value
        CHECK (
            btrim(event_id) <> ''
            AND btrim(event_type) <> ''
            AND payload_digest ~ '^[0-9a-fA-F]{64}$'
            AND is_deleted = false
            AND version >= 0
        )
);

ALTER SEQUENCE public.auth_consumed_event_seq OWNED BY public.auth_consumed_event.id;

CREATE INDEX idx_auth_consumed_event_time
    ON public.auth_consumed_event (consumed_time DESC, id DESC);

COMMENT ON TABLE public.auth_consumed_event IS 'Auth 对 IAM 可靠事件的幂等消费记录';

CREATE TABLE public.auth_audit_log (
    id bigint NOT NULL DEFAULT nextval('public.auth_audit_log_seq'::regclass),
    tenant_id bigint,
    subject_type varchar(32),
    subject_id bigint,
    operator_type varchar(32) NOT NULL,
    operator_id bigint NOT NULL,
    action_code varchar(128) NOT NULL,
    result varchar(32) NOT NULL,
    login_method varchar(32),
    failure_reason_code varchar(128),
    masked_mobile varchar(32),
    ip_hash varchar(64),
    device_summary varchar(256),
    trace_id varchar(128),
    occurred_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_audit_log PRIMARY KEY (id),
    CONSTRAINT ck_auth_audit_log_subject_type
        CHECK (subject_type IS NULL OR subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_auth_audit_log_operator_type
        CHECK (operator_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT', 'ANONYMOUS', 'SYSTEM')),
    CONSTRAINT ck_auth_audit_log_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT ck_auth_audit_log_login_method
        CHECK (login_method IS NULL OR login_method IN ('PASSWORD', 'SMS', 'PASSWORD_SMS', 'RECOVERY_CODE')),
    CONSTRAINT ck_auth_audit_log_value
        CHECK (
            operator_id >= 0
            AND btrim(action_code) <> ''
            AND (ip_hash IS NULL OR ip_hash ~ '^[0-9a-fA-F]{64}$')
            AND is_deleted = false
            AND version >= 0
        )
);

ALTER SEQUENCE public.auth_audit_log_seq OWNED BY public.auth_audit_log.id;

CREATE INDEX idx_auth_audit_log_tenant_time
    ON public.auth_audit_log (tenant_id, occurred_time DESC, id DESC);

CREATE INDEX idx_auth_audit_log_subject_time
    ON public.auth_audit_log (subject_type, subject_id, occurred_time DESC, id DESC);

CREATE INDEX idx_auth_audit_log_action_time
    ON public.auth_audit_log (action_code, occurred_time DESC, id DESC);

COMMENT ON TABLE public.auth_audit_log IS '登录、验证码、凭证、手机号和设备会话只追加安全审计';
COMMENT ON COLUMN public.auth_audit_log.masked_mobile IS '仅允许脱敏手机号，禁止完整手机号';

CREATE TABLE public.auth_idempotency_record (
    id bigint NOT NULL DEFAULT nextval('public.auth_idempotency_record_seq'::regclass),
    subject_type varchar(32),
    subject_id bigint,
    operation_code varchar(128) NOT NULL,
    idempotency_key_hash varchar(64) NOT NULL,
    request_hash varchar(64) NOT NULL,
    response_reference varchar(256),
    status varchar(32) NOT NULL DEFAULT 'PROCESSING',
    expires_at timestamptz NOT NULL,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_idempotency_record PRIMARY KEY (id),
    CONSTRAINT uk_auth_idempotency_record_key UNIQUE (operation_code, idempotency_key_hash),
    CONSTRAINT ck_auth_idempotency_record_subject_type
        CHECK (subject_type IS NULL OR subject_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT')),
    CONSTRAINT ck_auth_idempotency_record_status
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_auth_idempotency_record_value
        CHECK (
            btrim(operation_code) <> ''
            AND idempotency_key_hash ~ '^[0-9a-fA-F]{64}$'
            AND request_hash ~ '^[0-9a-fA-F]{64}$'
            AND expires_at > created_time
            AND version >= 0
        )
);

ALTER SEQUENCE public.auth_idempotency_record_seq OWNED BY public.auth_idempotency_record.id;

CREATE INDEX idx_auth_idempotency_record_expiration
    ON public.auth_idempotency_record (expires_at, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.auth_idempotency_record IS '登录外敏感写命令的幂等请求摘要，禁止保存 Token 或一次性链接';

COMMIT;
