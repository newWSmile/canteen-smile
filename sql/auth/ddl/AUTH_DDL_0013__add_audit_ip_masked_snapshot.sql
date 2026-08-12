-- script_id: AUTH_DDL_0013
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0012
-- transactional: YES
-- purpose: 为认证安全审计增加来源 IP 脱敏展示快照，原始 IP 仍不落库
-- rollback: 开发阶段可删除 ip_masked 字段；共享环境执行后禁止回滚修改已发布脚本

BEGIN;

ALTER TABLE public.auth_audit_log
    ADD COLUMN ip_masked varchar(128);

COMMENT ON COLUMN public.auth_audit_log.ip_masked IS
    '登录或认证安全事件来源 IP 的脱敏展示快照，IPv4 隐藏最后一段，禁止保存原始完整 IP';

COMMIT;

-- verification:
-- SELECT column_name, col_description('public.auth_audit_log'::regclass, ordinal_position)
-- FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'auth_audit_log' AND column_name = 'ip_masked';
