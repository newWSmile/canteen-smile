package com.canteen.smile.modules.auth.model;

/** Auth 已冻结的主体、应用、会话和算法常量。 */
public final class AuthConstants {

    /** 平台管理端应用编码。 */
    public static final String PLATFORM_ADMIN_APP = "PLATFORM_ADMIN";

    /** 租户管理端应用编码。 */
    public static final String TENANT_ADMIN_APP = "TENANT_ADMIN";

    /** 租户用户端应用编码。 */
    public static final String TENANT_PORTAL_APP = "TENANT_PORTAL";

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

    /** Token Session 中的租户 ID 属性名。 */
    public static final String TOKEN_TENANT_ID_ATTRIBUTE = "tenantId";

    /** Token Session 中的当前用户名属性名。 */
    public static final String TOKEN_USERNAME_ATTRIBUTE = "username";

    /** Token Session 中的当前显示名称属性名。 */
    public static final String TOKEN_DISPLAY_NAME_ATTRIBUTE = "displayName";

    /** Token Session 中的当前机构 ID 属性名。 */
    public static final String TOKEN_ORGANIZATION_ID_ATTRIBUTE = "organizationId";

    /** Token Session 中的当前应用编码属性名。 */
    public static final String TOKEN_APP_CODE_ATTRIBUTE = "appCode";

    /** 恢复码二次验证登录方式。 */
    public static final String RECOVERY_CODE_LOGIN_METHOD = "RECOVERY_CODE";

    /** 用户名密码登录方式。 */
    public static final String PASSWORD_LOGIN_METHOD = "PASSWORD";

    /** 手机号验证码登录方式。 */
    public static final String SMS_LOGIN_METHOD = "SMS";

    /** 禁止实例化常量类。 */
    private AuthConstants() {
    }
}
