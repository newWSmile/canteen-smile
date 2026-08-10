-- script_id: AUTH_DDL_0005
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0004
-- transactional: YES
-- purpose: 为 Auth 安全审计增加认证主体与操作人的不可变身份快照
-- rollback: 共享环境执行后禁止回滚或删除字段，只能新增更高编号修正脚本

BEGIN;

ALTER TABLE public.auth_audit_log
    ADD COLUMN subject_username_snapshot varchar(128),
    ADD COLUMN subject_display_name_snapshot varchar(128),
    ADD COLUMN operator_username_snapshot varchar(128),
    ADD COLUMN operator_display_name_snapshot varchar(128),
    ADD CONSTRAINT ck_auth_audit_log_identity_snapshots
        CHECK (
            (subject_username_snapshot IS NULL OR btrim(subject_username_snapshot) <> '')
            AND (subject_display_name_snapshot IS NULL OR btrim(subject_display_name_snapshot) <> '')
            AND (operator_username_snapshot IS NULL OR btrim(operator_username_snapshot) <> '')
            AND (operator_display_name_snapshot IS NULL OR btrim(operator_display_name_snapshot) <> '')
        );

COMMENT ON COLUMN public.auth_audit_log.subject_username_snapshot IS '认证或会话安全事件发生时主体用户名快照，后续改名不影响历史记录';
COMMENT ON COLUMN public.auth_audit_log.subject_display_name_snapshot IS '认证或会话安全事件发生时主体显示名称快照，为空时前端使用用户名';
COMMENT ON COLUMN public.auth_audit_log.operator_username_snapshot IS '认证安全事件发生时操作人用户名快照，匿名或系统操作可以为空';
COMMENT ON COLUMN public.auth_audit_log.operator_display_name_snapshot IS '认证安全事件发生时操作人显示名称快照，匿名或系统操作可以为空';

COMMIT;

-- 执行后核对：四个新增字段均存在且具有中文注释。
-- SELECT column_name, col_description('public.auth_audit_log'::regclass, ordinal_position)
-- FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'auth_audit_log'
--   AND column_name LIKE '%snapshot'
-- ORDER BY ordinal_position;
