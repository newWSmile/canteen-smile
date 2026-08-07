package com.canteen.smile.modules.auth.model;

/** 密码信封允许绑定的业务用途，禁止跨接口复用挑战。 */
public enum PasswordEnvelopePurpose {

    /** 首位平台超级管理员初始化密码。 */
    PLATFORM_BOOTSTRAP,

    /** 平台超级管理员用户名密码登录。 */
    PLATFORM_PASSWORD_LOGIN,

    /** 租户账号使用一次性激活票据设置初始密码。 */
    TENANT_ACCOUNT_ACTIVATION,

    /** 租户账号用户名密码登录。 */
    TENANT_PASSWORD_LOGIN,

    /** 平台身份为敏感管理操作再次验证当前密码。 */
    PLATFORM_REAUTH_PASSWORD,

    /** 租户账号通过一次性恢复链接设置新密码。 */
    TENANT_PASSWORD_RESET
}
