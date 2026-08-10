-- script_id: AUTH_DDL_0009
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0008
-- transactional: YES
-- purpose: 创建平台可维护的短信验证码限流与敏感内容留存策略，并标记投递记录是否包含敏感正文
-- rollback: 仅允许在从未承载共享环境数据时整体回滚；共享环境执行后必须新增更高编号修正脚本

BEGIN;

CREATE SEQUENCE public.auth_sms_runtime_policy_seq AS bigint;

CREATE TABLE public.auth_sms_runtime_policy (
    id bigint NOT NULL DEFAULT nextval('public.auth_sms_runtime_policy_seq'::regclass),
    policy_code varchar(64) NOT NULL,
    challenge_ttl_seconds integer NOT NULL,
    resend_interval_seconds integer NOT NULL,
    max_verification_attempts integer NOT NULL,
    mobile_hourly_limit integer NOT NULL,
    mobile_daily_limit integer NOT NULL,
    ip_hourly_limit integer NOT NULL,
    ip_daily_limit integer NOT NULL,
    device_hourly_limit integer NOT NULL,
    device_daily_limit integer NOT NULL,
    plaintext_code_retention_enabled boolean NOT NULL DEFAULT false,
    created_by bigint,
    created_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    updated_time timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_sms_runtime_policy PRIMARY KEY (id),
    CONSTRAINT uk_auth_sms_runtime_policy_code UNIQUE (policy_code),
    CONSTRAINT ck_auth_sms_runtime_policy_singleton CHECK (policy_code = 'GLOBAL'),
    CONSTRAINT ck_auth_sms_runtime_policy_challenge
        CHECK (
            challenge_ttl_seconds BETWEEN 60 AND 900
            AND resend_interval_seconds BETWEEN 30 AND 600
            AND max_verification_attempts BETWEEN 1 AND 5
        ),
    CONSTRAINT ck_auth_sms_runtime_policy_rate_limit
        CHECK (
            mobile_hourly_limit BETWEEN 1 AND 100
            AND mobile_daily_limit BETWEEN mobile_hourly_limit AND 500
            AND ip_hourly_limit BETWEEN 1 AND 1000
            AND ip_daily_limit BETWEEN ip_hourly_limit AND 5000
            AND device_hourly_limit BETWEEN 1 AND 500
            AND device_daily_limit BETWEEN device_hourly_limit AND 2000
        ),
    CONSTRAINT ck_auth_sms_runtime_policy_version CHECK (version >= 0)
);

ALTER SEQUENCE public.auth_sms_runtime_policy_seq
    OWNED BY public.auth_sms_runtime_policy.id;

ALTER TABLE public.auth_sms_delivery_record
    ADD COLUMN sensitive_content_retained boolean NOT NULL DEFAULT false;

COMMENT ON TABLE public.auth_sms_runtime_policy IS '平台统一维护的短信验证码有效期、错误次数、发送限流和敏感正文留存策略；仅允许一条 GLOBAL 策略';
COMMENT ON COLUMN public.auth_sms_runtime_policy.id IS '本表记录的主键 ID';
COMMENT ON COLUMN public.auth_sms_runtime_policy.policy_code IS '短信运行策略编码，当前固定为全局策略 GLOBAL';
COMMENT ON COLUMN public.auth_sms_runtime_policy.challenge_ttl_seconds IS '短信验证码有效秒数';
COMMENT ON COLUMN public.auth_sms_runtime_policy.resend_interval_seconds IS '同一手机号再次发送验证码前必须等待的秒数';
COMMENT ON COLUMN public.auth_sms_runtime_policy.max_verification_attempts IS '单个验证码允许的最大错误校验次数';
COMMENT ON COLUMN public.auth_sms_runtime_policy.mobile_hourly_limit IS '同一手机号每小时允许发送验证码的最大次数';
COMMENT ON COLUMN public.auth_sms_runtime_policy.mobile_daily_limit IS '同一手机号每日允许发送验证码的最大次数';
COMMENT ON COLUMN public.auth_sms_runtime_policy.ip_hourly_limit IS '同一来源 IP 每小时允许发送验证码的最大次数';
COMMENT ON COLUMN public.auth_sms_runtime_policy.ip_daily_limit IS '同一来源 IP 每日允许发送验证码的最大次数';
COMMENT ON COLUMN public.auth_sms_runtime_policy.device_hourly_limit IS '同一设备每小时允许发送验证码的最大次数';
COMMENT ON COLUMN public.auth_sms_runtime_policy.device_daily_limit IS '同一设备每日允许发送验证码的最大次数';
COMMENT ON COLUMN public.auth_sms_runtime_policy.plaintext_code_retention_enabled IS '是否允许投递记录保留验证码明文；默认关闭，开启后仅影响后续新记录';
COMMENT ON COLUMN public.auth_sms_runtime_policy.created_by IS '创建策略的平台身份 ID，系统首次生成默认策略时允许为空';
COMMENT ON COLUMN public.auth_sms_runtime_policy.created_time IS '策略创建时间';
COMMENT ON COLUMN public.auth_sms_runtime_policy.updated_by IS '最后修改策略的平台身份 ID';
COMMENT ON COLUMN public.auth_sms_runtime_policy.updated_time IS '策略最后修改时间';
COMMENT ON COLUMN public.auth_sms_runtime_policy.is_deleted IS '逻辑删除标记，全局短信策略不允许业务删除';
COMMENT ON COLUMN public.auth_sms_runtime_policy.version IS '更新短信策略时使用的乐观锁版本';

COMMENT ON TABLE public.auth_sms_delivery_record IS '短信发送尝试的投递记录；是否保留验证码等敏感正文由平台短信安全策略决定，完整手机号始终禁止保存';
COMMENT ON COLUMN public.auth_sms_delivery_record.content_snapshot IS '短信展示正文快照；默认隐藏验证码，安全策略显式开启后允许后续记录保留验证码明文';
COMMENT ON COLUMN public.auth_sms_delivery_record.sensitive_content_retained IS '正文快照是否按当时安全策略保留验证码等敏感内容';

COMMIT;
