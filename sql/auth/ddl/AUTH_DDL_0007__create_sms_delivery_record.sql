-- script_id: AUTH_DDL_0007
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0006
-- transactional: YES
-- purpose: 创建短信发送尝试的脱敏投递记录，支持本地日志策略和后续真实供应商策略统一审计
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.auth_sms_delivery_record_seq AS bigint;

CREATE TABLE public.auth_sms_delivery_record (
    id bigint NOT NULL DEFAULT nextval('public.auth_sms_delivery_record_seq'::regclass),
    request_id varchar(64) NOT NULL,
    challenge_id varchar(64),
    provider_code varchar(64) NOT NULL,
    purpose varchar(64) NOT NULL,
    masked_mobile varchar(32) NOT NULL,
    mobile_hash varchar(64) NOT NULL,
    template_code varchar(128),
    content_snapshot varchar(1000) NOT NULL,
    status varchar(32) NOT NULL,
    provider_message_id varchar(128),
    failure_code varchar(64),
    failure_message varchar(500),
    accepted_time timestamptz,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_sms_delivery_record PRIMARY KEY (id),
    CONSTRAINT uk_auth_sms_delivery_record_request UNIQUE (request_id),
    CONSTRAINT ck_auth_sms_delivery_record_purpose
        CHECK (
            purpose IN (
                'LOGIN', 'ACTIVATION', 'PASSWORD_RESET', 'MOBILE_BIND',
                'MOBILE_CHANGE', 'ADMIN_REAUTH', 'PLATFORM_SECOND_FACTOR'
            )
        ),
    CONSTRAINT ck_auth_sms_delivery_record_status
        CHECK (status IN ('PROCESSING', 'ACCEPTED', 'FAILED')),
    CONSTRAINT ck_auth_sms_delivery_record_value
        CHECK (
            btrim(request_id) <> ''
            AND (challenge_id IS NULL OR btrim(challenge_id) <> '')
            AND btrim(provider_code) <> ''
            AND btrim(masked_mobile) <> ''
            AND mobile_hash ~ '^[0-9a-fA-F]{64}$'
            AND (template_code IS NULL OR btrim(template_code) <> '')
            AND btrim(content_snapshot) <> ''
            AND (provider_message_id IS NULL OR btrim(provider_message_id) <> '')
            AND (failure_code IS NULL OR btrim(failure_code) <> '')
            AND (failure_message IS NULL OR btrim(failure_message) <> '')
            AND version >= 0
        ),
    CONSTRAINT ck_auth_sms_delivery_record_result
        CHECK (
            (status = 'PROCESSING' AND accepted_time IS NULL AND provider_message_id IS NULL
                AND failure_code IS NULL AND failure_message IS NULL)
            OR
            (status = 'ACCEPTED' AND accepted_time IS NOT NULL AND failure_code IS NULL AND failure_message IS NULL)
            OR
            (status = 'FAILED' AND accepted_time IS NULL AND provider_message_id IS NULL)
        )
);

ALTER SEQUENCE public.auth_sms_delivery_record_seq
    OWNED BY public.auth_sms_delivery_record.id;

CREATE INDEX idx_auth_sms_delivery_record_challenge
    ON public.auth_sms_delivery_record (challenge_id, id)
    WHERE challenge_id IS NOT NULL AND is_deleted = false;

CREATE INDEX idx_auth_sms_delivery_record_mobile_time
    ON public.auth_sms_delivery_record (mobile_hash, created_time DESC, id DESC)
    WHERE is_deleted = false;

COMMENT ON TABLE public.auth_sms_delivery_record IS '短信发送尝试的投递记录；正文保持明文可读，禁止保存完整手机号、验证码、Token 和一次性链接';
COMMENT ON COLUMN public.auth_sms_delivery_record.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_sms_delivery_record.request_id IS '一次短信发送请求的全局唯一标识，用于防止重复投递';
COMMENT ON COLUMN public.auth_sms_delivery_record.challenge_id IS '关联的短信验证码挑战标识，非验证码短信允许为空';
COMMENT ON COLUMN public.auth_sms_delivery_record.provider_code IS '实际处理发送请求的短信策略或供应商编码';
COMMENT ON COLUMN public.auth_sms_delivery_record.purpose IS '短信发送的业务用途，例如登录、绑定手机号或密码找回';
COMMENT ON COLUMN public.auth_sms_delivery_record.masked_mobile IS '供审计和排障使用的脱敏手机号，禁止保存完整手机号';
COMMENT ON COLUMN public.auth_sms_delivery_record.mobile_hash IS '带服务端 Pepper 的手机号查询摘要';
COMMENT ON COLUMN public.auth_sms_delivery_record.template_code IS '短信供应商模板编码，本地策略或无需模板时允许为空';
COMMENT ON COLUMN public.auth_sms_delivery_record.content_snapshot IS '短信展示正文快照，验证码、Token 和一次性秘密必须替换为星号后保存';
COMMENT ON COLUMN public.auth_sms_delivery_record.status IS '发送请求状态，PROCESSING 表示已原子领取，ACCEPTED 表示已被策略接受，FAILED 表示处理失败';
COMMENT ON COLUMN public.auth_sms_delivery_record.provider_message_id IS '短信供应商返回的消息标识，本地策略使用本地请求标识';
COMMENT ON COLUMN public.auth_sms_delivery_record.failure_code IS '发送失败时的稳定内部失败编码，不保存供应商密钥或敏感报文';
COMMENT ON COLUMN public.auth_sms_delivery_record.failure_message IS '发送失败时经过脱敏和截断的排障说明';
COMMENT ON COLUMN public.auth_sms_delivery_record.accepted_time IS '短信发送请求被当前策略接受的时间';
COMMENT ON COLUMN public.auth_sms_delivery_record.created_by IS '创建该记录的身份 ID，系统发送使用约定的系统身份';
COMMENT ON COLUMN public.auth_sms_delivery_record.created_time IS '记录创建时间';
COMMENT ON COLUMN public.auth_sms_delivery_record.updated_by IS '最后修改该记录的身份 ID';
COMMENT ON COLUMN public.auth_sms_delivery_record.updated_time IS '记录最后更新时间';
COMMENT ON COLUMN public.auth_sms_delivery_record.is_deleted IS '逻辑删除标记，短信投递审计记录通常保持 false';
COMMENT ON COLUMN public.auth_sms_delivery_record.version IS '并发更新控制使用的乐观锁版本';

COMMIT;
