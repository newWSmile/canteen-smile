-- script_id: IAM_DDL_0007
-- target_database: IAM
-- type: DDL
-- depends_on: IAM_DDL_0006
-- transactional: YES
-- purpose: 为 Outbox 投递处理中租约超时恢复增加局部索引
-- rollback: 共享环境执行后仅允许通过更高编号脚本调整；开发环境可删除本脚本新增索引

BEGIN;

CREATE INDEX idx_iam_outbox_event_processing_lease
    ON public.iam_outbox_event (updated_time, id)
    WHERE status = 'PROCESSING' AND is_deleted = false;

COMMIT;

-- 验证：应返回 idx_iam_outbox_event_processing_lease。
-- SELECT indexname FROM pg_indexes
-- WHERE schemaname = 'public' AND tablename = 'iam_outbox_event'
--   AND indexname = 'idx_iam_outbox_event_processing_lease';
