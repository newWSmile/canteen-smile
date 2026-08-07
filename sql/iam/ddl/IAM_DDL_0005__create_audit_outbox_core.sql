-- script_id: IAM_DDL_0005
-- target_database: IAM
-- type: DDL
-- depends_on: IAM_DDL_0004
-- transactional: YES
-- purpose: 创建 IAM 管理审计、可靠事件、幂等记录和平台查看租户审计临时授权结构
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.iam_audit_log_seq AS bigint;
CREATE SEQUENCE public.iam_outbox_event_seq AS bigint;
CREATE SEQUENCE public.iam_idempotency_record_seq AS bigint;
CREATE SEQUENCE public.iam_tenant_audit_access_grant_seq AS bigint;

CREATE TABLE public.iam_audit_log (
    id bigint NOT NULL DEFAULT nextval('public.iam_audit_log_seq'::regclass),
    tenant_id bigint,
    operator_type varchar(32) NOT NULL,
    operator_id bigint NOT NULL,
    operator_organization_id bigint,
    action_code varchar(128) NOT NULL,
    target_type varchar(64) NOT NULL,
    target_id varchar(128) NOT NULL,
    reason varchar(500),
    result varchar(32) NOT NULL,
    masked_diff_json jsonb,
    ip_hash varchar(128),
    device_summary varchar(256),
    trace_id varchar(128),
    occurred_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_audit_log PRIMARY KEY (id),
    CONSTRAINT ck_iam_audit_log_operator_type
        CHECK (operator_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT', 'SYSTEM')),
    CONSTRAINT ck_iam_audit_log_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT ck_iam_audit_log_value
        CHECK (
            operator_id >= 0
            AND btrim(action_code) <> ''
            AND btrim(target_type) <> ''
            AND btrim(target_id) <> ''
            AND (reason IS NULL OR btrim(reason) <> '')
            AND (masked_diff_json IS NULL OR jsonb_typeof(masked_diff_json) = 'object')
            AND is_deleted = false
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_audit_log_seq
    OWNED BY public.iam_audit_log.id;

CREATE INDEX idx_iam_audit_log_tenant_time
    ON public.iam_audit_log (tenant_id, occurred_time DESC, id DESC);

CREATE INDEX idx_iam_audit_log_operator_time
    ON public.iam_audit_log (operator_type, operator_id, occurred_time DESC, id DESC);

CREATE INDEX idx_iam_audit_log_target_time
    ON public.iam_audit_log (target_type, target_id, occurred_time DESC, id DESC);

COMMENT ON TABLE public.iam_audit_log IS 'IAM 管理操作只追加审计；敏感差异必须在写入前脱敏';
COMMENT ON COLUMN public.iam_audit_log.operator_id IS 'SYSTEM 操作使用 0，其它类型使用真实身份 ID';
COMMENT ON COLUMN public.iam_audit_log.masked_diff_json IS '只允许非敏感字段白名单和脱敏值';

CREATE TABLE public.iam_outbox_event (
    id bigint NOT NULL DEFAULT nextval('public.iam_outbox_event_seq'::regclass),
    event_id varchar(64) NOT NULL,
    aggregate_type varchar(64) NOT NULL,
    aggregate_id varchar(128) NOT NULL,
    tenant_id bigint,
    event_type varchar(128) NOT NULL,
    schema_version integer NOT NULL DEFAULT 1,
    payload_json jsonb NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'PENDING',
    retry_count integer NOT NULL DEFAULT 0,
    next_retry_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error_code varchar(128),
    trace_id varchar(128),
    occurred_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_outbox_event PRIMARY KEY (id),
    CONSTRAINT uk_iam_outbox_event_event_id UNIQUE (event_id),
    CONSTRAINT ck_iam_outbox_event_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY', 'PUBLISHED', 'DEAD')),
    CONSTRAINT ck_iam_outbox_event_value
        CHECK (
            btrim(event_id) <> ''
            AND btrim(aggregate_type) <> ''
            AND btrim(aggregate_id) <> ''
            AND btrim(event_type) <> ''
            AND schema_version > 0
            AND jsonb_typeof(payload_json) = 'object'
            AND retry_count >= 0
            AND version >= 0
        ),
    CONSTRAINT ck_iam_outbox_event_publish_state
        CHECK (
            (status = 'PUBLISHED' AND published_time IS NOT NULL)
            OR (status <> 'PUBLISHED')
        )
);

ALTER SEQUENCE public.iam_outbox_event_seq
    OWNED BY public.iam_outbox_event.id;

CREATE INDEX idx_iam_outbox_event_delivery
    ON public.iam_outbox_event (status, next_retry_time, id)
    WHERE status IN ('PENDING', 'RETRY') AND is_deleted = false;

CREATE INDEX idx_iam_outbox_event_aggregate
    ON public.iam_outbox_event (aggregate_type, aggregate_id, occurred_time DESC, id DESC);

COMMENT ON TABLE public.iam_outbox_event IS '与 IAM 业务数据同事务写入的可靠事件源';
COMMENT ON COLUMN public.iam_outbox_event.payload_json IS '最小事件载荷，禁止密码、验证码、Token、密钥和完整一次性链接';

CREATE TABLE public.iam_idempotency_record (
    id bigint NOT NULL DEFAULT nextval('public.iam_idempotency_record_seq'::regclass),
    tenant_id bigint,
    operator_type varchar(32) NOT NULL,
    operator_id bigint NOT NULL,
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
    CONSTRAINT pk_iam_idempotency_record PRIMARY KEY (id),
    CONSTRAINT uk_iam_idempotency_record_key
        UNIQUE (operator_type, operator_id, operation_code, idempotency_key_hash),
    CONSTRAINT ck_iam_idempotency_record_operator_type
        CHECK (operator_type IN ('PLATFORM_IDENTITY', 'TENANT_ACCOUNT', 'SYSTEM')),
    CONSTRAINT ck_iam_idempotency_record_status
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_iam_idempotency_record_value
        CHECK (
            operator_id >= 0
            AND btrim(operation_code) <> ''
            AND idempotency_key_hash ~ '^[0-9a-fA-F]{64}$'
            AND request_hash ~ '^[0-9a-fA-F]{64}$'
            AND expires_at > created_time
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_idempotency_record_seq
    OWNED BY public.iam_idempotency_record.id;

CREATE INDEX idx_iam_idempotency_record_expiration
    ON public.iam_idempotency_record (expires_at, id)
    WHERE is_deleted = false;

COMMENT ON TABLE public.iam_idempotency_record IS '敏感写命令的请求摘要和结果引用，禁止保存完整响应秘密';

CREATE TABLE public.iam_tenant_audit_access_grant (
    id bigint NOT NULL DEFAULT nextval('public.iam_tenant_audit_access_grant_seq'::regclass),
    tenant_id bigint NOT NULL,
    platform_identity_id bigint NOT NULL,
    reason varchar(500) NOT NULL,
    reauth_reference_hash varchar(64) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at timestamptz NOT NULL,
    revoked_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_iam_tenant_audit_access_grant PRIMARY KEY (id),
    CONSTRAINT fk_iam_tenant_audit_access_grant_tenant
        FOREIGN KEY (tenant_id) REFERENCES public.iam_tenant (id) ON DELETE RESTRICT,
    CONSTRAINT fk_iam_tenant_audit_access_grant_platform
        FOREIGN KEY (platform_identity_id) REFERENCES public.iam_platform_identity (id) ON DELETE RESTRICT,
    CONSTRAINT ck_iam_tenant_audit_access_grant_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_iam_tenant_audit_access_grant_value
        CHECK (
            btrim(reason) <> ''
            AND reauth_reference_hash ~ '^[0-9a-fA-F]{64}$'
            AND expires_at > created_time
            AND (
                (status = 'REVOKED' AND revoked_time IS NOT NULL)
                OR (status <> 'REVOKED')
            )
            AND version >= 0
        )
);

ALTER SEQUENCE public.iam_tenant_audit_access_grant_seq
    OWNED BY public.iam_tenant_audit_access_grant.id;

CREATE INDEX idx_iam_tenant_audit_access_grant_active
    ON public.iam_tenant_audit_access_grant (platform_identity_id, tenant_id, expires_at, id)
    WHERE status = 'ACTIVE' AND is_deleted = false;

COMMENT ON TABLE public.iam_tenant_audit_access_grant IS '平台身份经原因和再认证后查看租户审计的短期授权';

COMMIT;
