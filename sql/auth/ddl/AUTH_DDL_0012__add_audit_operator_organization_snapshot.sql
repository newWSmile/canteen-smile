-- Canteen Smile / Auth 审计操作人机构快照
-- PostgreSQL；依赖 AUTH_DDL_0011。

BEGIN;

ALTER TABLE public.auth_audit_log
    ADD COLUMN operator_organization_id bigint,
    ADD CONSTRAINT ck_auth_audit_log_operator_organization CHECK (
        operator_organization_id IS NULL OR operator_organization_id > 0
    );

CREATE INDEX idx_auth_audit_log_tenant_operator_org_time
    ON public.auth_audit_log (
        tenant_id,
        operator_organization_id,
        occurred_time DESC,
        id DESC
    )
    WHERE operator_organization_id IS NOT NULL AND is_deleted = false;

COMMENT ON COLUMN public.auth_audit_log.operator_organization_id IS '认证安全事件发生时操作人所属机构 ID 快照，平台身份、匿名或系统操作可以为空';

COMMIT;
