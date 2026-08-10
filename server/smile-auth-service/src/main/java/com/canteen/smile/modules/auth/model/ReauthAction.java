package com.canteen.smile.modules.auth.model;

/**
 * Auth 允许签发再认证票据的敏感操作，票据不得跨操作复用。
 */
public enum ReauthAction {

    /** 平台身份为租户根机构所有者发起密码恢复。 */
    TENANT_OWNER_PASSWORD_RESET,

    /** 租户管理员创建账号并授予初始角色。 */
    TENANT_USER_CREATE,

    /** 租户管理员替换账号角色集合。 */
    TENANT_USER_ROLE_ASSIGN,

    /** 平台身份修改短信验证码限流或敏感内容留存策略。 */
    PLATFORM_SMS_POLICY_UPDATE
}
