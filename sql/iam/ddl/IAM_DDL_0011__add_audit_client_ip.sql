-- script_id: IAM_DDL_0011
-- target_database: IAM
-- type: DDL
-- depends_on: IAM_DDL_0010
-- transactional: YES
-- purpose: 为所有 IAM 管理操作审计增加完整客户端 IP 快照
-- rollback: 仅允许新增更高编号修正脚本

BEGIN;

ALTER TABLE public.iam_audit_log
    ADD COLUMN ip_address varchar(128);

COMMENT ON COLUMN public.iam_audit_log.ip_address IS
    '管理操作发生时由网关确认的完整客户端 IP，用于与账号、时间、设备及链路信息联合追查';

COMMIT;
