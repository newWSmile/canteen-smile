-- script_id: IAM_DDL_0009
-- target_database: IAM
-- type: DDL
-- depends_on: IAM_DDL_0008
-- transactional: YES
-- purpose: 为 IAM 管理审计增加事件发生时的中文动作名称快照
-- rollback: 共享环境执行后禁止删除字段，只能新增更高编号修正脚本

BEGIN;

ALTER TABLE public.iam_audit_log
    ADD COLUMN action_name_snapshot varchar(200),
    ADD CONSTRAINT ck_iam_audit_log_action_name_snapshot
        CHECK (action_name_snapshot IS NULL OR btrim(action_name_snapshot) <> '');

COMMENT ON COLUMN public.iam_audit_log.action_name_snapshot IS '审计事件发生时的中文动作名称快照；新事件必须写入，历史空值不进行回填或猜测';

COMMIT;

-- 执行后核对：字段存在且具有中文注释。
-- SELECT column_name, col_description('public.iam_audit_log'::regclass, ordinal_position)
-- FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'iam_audit_log'
--   AND column_name = 'action_name_snapshot';
