package com.canteen.smile.modules.auth.model;

/** Auth 已冻结的主体、应用、会话和算法常量。 */
public final class AuthConstants {

    /** 平台管理端应用编码。 */
    public static final String PLATFORM_ADMIN_APP = "PLATFORM_ADMIN";

    /** 租户管理端应用编码。 */
    public static final String TENANT_ADMIN_APP = "TENANT_ADMIN";

    /** 平台身份认证主体类型。 */
    public static final String PLATFORM_IDENTITY_SUBJECT = "PLATFORM_IDENTITY";

    /** 租户账号认证主体类型。 */
    public static final String TENANT_ACCOUNT_SUBJECT = "TENANT_ACCOUNT";

    /** 有效凭证状态。 */
    public static final String ACTIVE_STATUS = "ACTIVE";

    /** 平台 Sa-Token 登录 ID 前缀。 */
    public static final String PLATFORM_LOGIN_PREFIX = "PLATFORM:";

    /** 租户账号 Sa-Token 登录 ID 前缀。 */
    public static final String TENANT_LOGIN_PREFIX = "TENANT:";

    /** 恢复码二次验证登录方式。 */
    public static final String RECOVERY_CODE_LOGIN_METHOD = "RECOVERY_CODE";

    /** 用户名密码登录方式。 */
    public static final String PASSWORD_LOGIN_METHOD = "PASSWORD";

    /** 禁止实例化常量类。 */
    private AuthConstants() {
    }
}
