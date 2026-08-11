-- Canteen Smile / IAM 通用异步审计元数据
-- PostgreSQL；依赖 IAM_DDL_0009。

BEGIN;

ALTER TABLE public.iam_audit_log
    ADD COLUMN event_id varchar(64),
    ADD COLUMN schema_version integer,
    ADD COLUMN source_code varchar(64),
    ADD COLUMN category_path_snapshot jsonb,
    ADD COLUMN app_code_snapshot varchar(64),
    ADD COLUMN failure_reason_code varchar(128),
    ADD COLUMN duration_ms bigint,
    ADD CONSTRAINT ck_iam_audit_log_generic_metadata CHECK (
        (event_id IS NULL OR btrim(event_id) <> '')
        AND (schema_version IS NULL OR schema_version > 0)
        AND (source_code IS NULL OR btrim(source_code) <> '')
        AND (category_path_snapshot IS NULL OR jsonb_typeof(category_path_snapshot) = 'array')
        AND (app_code_snapshot IS NULL OR btrim(app_code_snapshot) <> '')
        AND (failure_reason_code IS NULL OR btrim(failure_reason_code) <> '')
        AND (duration_ms IS NULL OR duration_ms >= 0)
    );

CREATE UNIQUE INDEX uk_iam_audit_log_event_id
    ON public.iam_audit_log (event_id)
    WHERE event_id IS NOT NULL;

COMMENT ON COLUMN public.iam_audit_log.event_id IS '通用审计事件全局唯一 ID，用于异步投递和未来消息队列幂等消费';
COMMENT ON COLUMN public.iam_audit_log.schema_version IS '通用审计事件契约版本，用于兼容后续消息结构演进';
COMMENT ON COLUMN public.iam_audit_log.source_code IS '产生审计事件的服务或业务域稳定编码';
COMMENT ON COLUMN public.iam_audit_log.category_path_snapshot IS '与菜单无强关联的任意层级中文审计分类路径快照';
COMMENT ON COLUMN public.iam_audit_log.app_code_snapshot IS '事件发生时操作人所在的平台管理端、租户管理端或租户业务端应用编码快照';
COMMENT ON COLUMN public.iam_audit_log.failure_reason_code IS '业务拒绝或执行失败时写入的稳定错误码';
COMMENT ON COLUMN public.iam_audit_log.duration_ms IS '被审计业务方法从进入到完成的执行耗时毫秒数';

COMMIT;

