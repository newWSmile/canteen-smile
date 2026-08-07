package com.canteen.smile.modules.auth.model;

/**
 * Auth 允许签发再认证票据的敏感操作，票据不得跨操作复用。
 */
public enum ReauthAction {

    /** 平台身份为租户根机构所有者发起密码恢复。 */
    TENANT_OWNER_PASSWORD_RESET
}
