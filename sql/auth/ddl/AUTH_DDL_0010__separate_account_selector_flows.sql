-- script_id: AUTH_DDL_0010
-- target_database: AUTH
-- type: DDL
-- depends_on: AUTH_DDL_0009
-- transactional: YES
-- purpose: 为手机号多账号选择票据增加流程用途，隔离登录与自助找回密码
-- rollback: 仅允许新增更高编号修正脚本

BEGIN;

ALTER TABLE public.auth_account_selector_ticket
    ADD COLUMN flow_type varchar(32) NOT NULL DEFAULT 'LOGIN';

ALTER TABLE public.auth_account_selector_ticket
    ADD CONSTRAINT ck_auth_account_selector_ticket_flow_type
        CHECK (flow_type IN ('LOGIN', 'PASSWORD_RESET'));

COMMENT ON COLUMN public.auth_account_selector_ticket.flow_type
    IS '账号选择票据的唯一允许流程；登录与密码找回禁止交叉复用';

COMMIT;
