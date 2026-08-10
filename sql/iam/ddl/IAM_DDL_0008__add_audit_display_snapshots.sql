-- script_id: IAM_DDL_0008
-- target_database: IAM
-- type: DDL
-- depends_on: IAM_DDL_0007
-- transactional: YES
-- purpose: 为 IAM 管理审计增加操作人与目标的不可变展示快照
-- rollback: 共享环境执行后禁止回滚或删除字段，只能新增更高编号修正脚本

BEGIN;

ALTER TABLE public.iam_audit_log
    ADD COLUMN operator_username_snapshot varchar(128),
    ADD COLUMN operator_display_name_snapshot varchar(128),
    ADD COLUMN target_name_snapshot varchar(200),
    ADD COLUMN target_code_snapshot varchar(128),
    ADD CONSTRAINT ck_iam_audit_log_display_snapshots
        CHECK (
            (operator_username_snapshot IS NULL OR btrim(operator_username_snapshot) <> '')
            AND (operator_display_name_snapshot IS NULL OR btrim(operator_display_name_snapshot) <> '')
            AND (target_name_snapshot IS NULL OR btrim(target_name_snapshot) <> '')
            AND (target_code_snapshot IS NULL OR btrim(target_code_snapshot) <> '')
        );

COMMENT ON COLUMN public.iam_audit_log.operator_username_snapshot IS '审计事件发生时操作人的用户名快照，后续改名不影响历史记录';
COMMENT ON COLUMN public.iam_audit_log.operator_display_name_snapshot IS '审计事件发生时操作人的显示名称快照，为空时前端使用用户名';
COMMENT ON COLUMN public.iam_audit_log.target_name_snapshot IS '审计事件发生时被操作目标的中文名称快照，目标改名或注销后仍保留';
COMMENT ON COLUMN public.iam_audit_log.target_code_snapshot IS '审计事件发生时被操作目标的稳定业务编码或用户名快照';

COMMIT;

-- 执行后核对：四个新增字段均存在且具有中文注释。
-- SELECT column_name, col_description('public.iam_audit_log'::regclass, ordinal_position)
-- FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'iam_audit_log'
--   AND column_name LIKE '%snapshot'
-- ORDER BY ordinal_position;
