-- Canteen Smile / Auth 通用异步审计元数据
-- PostgreSQL；依赖 AUTH_DDL_0010。

BEGIN;

ALTER TABLE public.auth_audit_log
    ADD COLUMN event_id varchar(64),
    ADD COLUMN schema_version integer,
    ADD COLUMN source_code varchar(64),
    ADD COLUMN category_path_snapshot jsonb,
    ADD COLUMN app_code_snapshot varchar(64),
    ADD COLUMN target_type varchar(64),
    ADD COLUMN target_id varchar(128),
    ADD COLUMN target_name_snapshot varchar(200),
    ADD COLUMN target_code_snapshot varchar(128),
    ADD COLUMN reason varchar(500),
    ADD COLUMN duration_ms bigint,
    ADD CONSTRAINT ck_auth_audit_log_generic_metadata CHECK (
        (event_id IS NULL OR btrim(event_id) <> '')
        AND (schema_version IS NULL OR schema_version > 0)
        AND (source_code IS NULL OR btrim(source_code) <> '')
        AND (category_path_snapshot IS NULL OR jsonb_typeof(category_path_snapshot) = 'array')
        AND (app_code_snapshot IS NULL OR btrim(app_code_snapshot) <> '')
        AND (target_type IS NULL OR btrim(target_type) <> '')
        AND (target_id IS NULL OR btrim(target_id) <> '')
        AND (reason IS NULL OR btrim(reason) <> '')
        AND (duration_ms IS NULL OR duration_ms >= 0)
    );

CREATE UNIQUE INDEX uk_auth_audit_log_event_id
    ON public.auth_audit_log (event_id)
    WHERE event_id IS NOT NULL;

COMMENT ON COLUMN public.auth_audit_log.event_id IS '通用审计事件全局唯一 ID，用于异步投递和未来消息队列幂等消费';
COMMENT ON COLUMN public.auth_audit_log.schema_version IS '通用审计事件契约版本，用于兼容后续消息结构演进';
COMMENT ON COLUMN public.auth_audit_log.source_code IS '产生审计事件的服务或业务域稳定编码';
COMMENT ON COLUMN public.auth_audit_log.category_path_snapshot IS '与菜单无强关联的任意层级中文审计分类路径快照';
COMMENT ON COLUMN public.auth_audit_log.app_code_snapshot IS '事件发生时操作人所在的平台管理端、租户管理端或租户业务端应用编码快照';
COMMENT ON COLUMN public.auth_audit_log.target_type IS '通用审计事件被操作目标的业务类型';
COMMENT ON COLUMN public.auth_audit_log.target_id IS '通用审计事件被操作目标的业务 ID';
COMMENT ON COLUMN public.auth_audit_log.target_name_snapshot IS '事件发生时被操作目标的中文名称快照';
COMMENT ON COLUMN public.auth_audit_log.target_code_snapshot IS '事件发生时被操作目标的稳定业务编码或用户名快照';
COMMENT ON COLUMN public.auth_audit_log.reason IS '敏感操作或业务变更的可选原因快照';
COMMENT ON COLUMN public.auth_audit_log.duration_ms IS '被审计业务方法从进入到完成的执行耗时毫秒数';

COMMIT;

