-- script_id: IAM_DML_0002
-- target_database: IAM
-- type: DML
-- depends_on: IAM_DDL_0007
-- transactional: YES
-- purpose: 将历史待投递的角色授权事件展开为受影响账号级事件
-- rollback: 不回滚；脚本幂等，已生成事件由 event_id 唯一约束保护

BEGIN;

INSERT INTO public.iam_outbox_event (
    event_id, aggregate_type, aggregate_id, tenant_id, event_type, schema_version,
    payload_json, status, retry_count, next_retry_time, trace_id, occurred_time,
    created_by, updated_by
)
SELECT
    'ra-' || md5(source_event.event_id || ':' || account.id::text),
    'TENANT_ACCOUNT', account.id::varchar, source_event.tenant_id,
    'ROLE_AUTHORIZATION_CHANGED', source_event.schema_version,
    jsonb_build_object(
        'tenantId', source_event.tenant_id::varchar,
        'roleId', source_event.aggregate_id,
        'accountId', account.id::varchar
    ),
    'PENDING', 0, CURRENT_TIMESTAMP, source_event.trace_id, source_event.occurred_time,
    source_event.created_by, source_event.updated_by
FROM public.iam_outbox_event source_event
JOIN public.iam_account_role account_role
  ON account_role.tenant_id = source_event.tenant_id
 AND account_role.role_id = CASE
       WHEN source_event.aggregate_id ~ '^[1-9][0-9]*$' THEN source_event.aggregate_id::bigint
       ELSE NULL
     END
 AND account_role.is_deleted = false
JOIN public.iam_account account
  ON account.tenant_id = account_role.tenant_id
 AND account.organization_id = account_role.organization_id
 AND account.id = account_role.account_id
 AND account.is_deleted = false
WHERE source_event.aggregate_type = 'ROLE'
  AND source_event.event_type = 'ROLE_AUTHORIZATION_CHANGED'
  AND source_event.status IN ('PENDING', 'RETRY')
  AND source_event.aggregate_id ~ '^[1-9][0-9]*$'
  AND source_event.is_deleted = false
ON CONFLICT (event_id) DO NOTHING;

UPDATE public.iam_outbox_event
SET status = 'PUBLISHED', published_time = CURRENT_TIMESTAMP,
    last_error_code = 'EXPANDED_TO_ACCOUNT_EVENTS', updated_by = 0,
    updated_time = CURRENT_TIMESTAMP, version = version + 1
WHERE aggregate_type = 'ROLE'
  AND event_type = 'ROLE_AUTHORIZATION_CHANGED'
  AND status IN ('PENDING', 'RETRY')
  AND aggregate_id ~ '^[1-9][0-9]*$'
  AND is_deleted = false;

COMMIT;

-- 验证：历史角色级待投递事件应为 0。
-- SELECT COUNT(*) FROM public.iam_outbox_event
-- WHERE aggregate_type = 'ROLE' AND event_type = 'ROLE_AUTHORIZATION_CHANGED'
--   AND status IN ('PENDING', 'RETRY') AND is_deleted = false;

-- 验证：账号级角色授权事件应包含 accountId、tenantId 和 roleId。
-- SELECT event_id, aggregate_id, payload_json, status
-- FROM public.iam_outbox_event
-- WHERE aggregate_type = 'TENANT_ACCOUNT' AND event_type = 'ROLE_AUTHORIZATION_CHANGED'
-- ORDER BY id DESC LIMIT 20;
