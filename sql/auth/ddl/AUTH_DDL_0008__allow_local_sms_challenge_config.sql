-- script_id: AUTH_DDL_0008
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0007
-- transactional: YES
-- purpose: 允许本地数据库日志短信策略在不初始化真实供应商和模板配置时创建验证码挑战
-- rollback: 仅在确认不存在空配置挑战记录时恢复 NOT NULL；共享环境执行后必须新增更高编号修正脚本

BEGIN;

ALTER TABLE public.auth_sms_challenge
    ALTER COLUMN provider_config_id DROP NOT NULL,
    ALTER COLUMN template_config_id DROP NOT NULL;

ALTER TABLE public.auth_sms_challenge
    ADD CONSTRAINT ck_auth_sms_challenge_config_pair
        CHECK (
            (provider_config_id IS NULL AND template_config_id IS NULL)
            OR (provider_config_id IS NOT NULL AND template_config_id IS NOT NULL)
        );

COMMENT ON COLUMN public.auth_sms_challenge.provider_config_id IS
    '本次发送使用的短信供应商配置 ID；本地数据库日志策略不依赖真实供应商配置时为空';
COMMENT ON COLUMN public.auth_sms_challenge.template_config_id IS
    '本次发送使用的短信模板配置 ID；本地数据库日志策略不依赖真实模板配置时为空';

COMMIT;
