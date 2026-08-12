-- script_id: AUTH_DDL_0014
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0013
-- transactional: YES
-- purpose: 登录设备与认证审计改为保存完整客户端 IP，支持受控安全追查
-- rollback: 仅允许新增更高编号修正脚本

BEGIN;

ALTER TABLE public.auth_device_session
    RENAME COLUMN login_ip_masked TO login_ip_address;

ALTER TABLE public.auth_audit_log
    RENAME COLUMN ip_masked TO ip_address;

COMMENT ON COLUMN public.auth_device_session.login_ip_address IS
    '登录时由网关确认的完整客户端 IP，仅允许当前账号在登录设备页及受权安全人员查看';

COMMENT ON COLUMN public.auth_audit_log.ip_address IS
    '认证安全事件发生时由网关确认的完整客户端 IP，用于与账号、时间、设备及链路信息联合追查';

COMMIT;
