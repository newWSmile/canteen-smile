package com.canteen.smile.modules.auth.model;

/**
 * Auth 允许签发再认证票据的敏感操作，票据不得跨操作复用。
 */
public enum ReauthAction {

    /** 平台身份为租户根机构所有者发起密码恢复。 */
    TENANT_OWNER_PASSWORD_RESET,

    /** 平台身份暂停、恢复或不可恢复注销租户。 */
    PLATFORM_TENANT_GOVERNANCE,

    /** 租户管理员创建账号并授予初始角色。 */
    TENANT_USER_CREATE,

    /** 租户管理员替换账号角色集合。 */
    TENANT_USER_ROLE_ASSIGN,

    /** 租户账号本人修改全局登录用户名。 */
    TENANT_USERNAME_CHANGE,

    /** 租户管理员为授权范围内账号发起一次性密码重置。 */
    TENANT_USER_PASSWORD_RESET,

    /** 当前机构所有者将机构所有权转让给本机构其他账号。 */
    TENANT_ORG_OWNER_TRANSFER,

    /** 租户根机构所有者调整功能停用或租户菜单隐藏配置。 */
    TENANT_NAVIGATION_UPDATE,

    /** 租户账号更换本人已验证手机号。 */
    MOBILE_CHANGE,

    /** 租户账号解绑本人已验证手机号。 */
    MOBILE_UNBIND,

    /** 平台身份修改短信验证码限流或敏感内容留存策略。 */
    PLATFORM_SMS_POLICY_UPDATE,

    /** 租户根机构所有者修改租户登录、会话、密码和审计保留策略。 */
    TENANT_SECURITY_POLICY_UPDATE
}
