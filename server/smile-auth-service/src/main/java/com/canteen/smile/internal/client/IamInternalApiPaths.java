package com.canteen.smile.internal.client;

/** Auth Client 固定使用的 IAM 内部 v1 路径。 */
public final class IamInternalApiPaths {

    /** IAM 内部 v1 根路径。 */
    public static final String INTERNAL_V1 = "/internal/iam/v1";

    /** 首位平台身份幂等创建路径。 */
    public static final String PLATFORM_IDENTITY_BOOTSTRAP = INTERNAL_V1 + "/platform-identities/bootstrap";

    /** 平台身份激活动作路径模板。 */
    public static final String PLATFORM_IDENTITY_ACTIVATE = INTERNAL_V1
            + "/platform-identities/{identityId}/actions/activate";

    /** 用户名登录主体解析路径。 */
    public static final String USERNAME_LOGIN_RESOLUTION = INTERNAL_V1 + "/login-resolutions/username";

    /** 租户账号激活上下文路径。 */
    public static final String TENANT_ACCOUNT_ACTIVATION_CONTEXT =
            INTERNAL_V1 + "/tenant-accounts/{accountId}/activation-context";

    /** 租户账号激活状态同步路径。 */
    public static final String TENANT_ACCOUNT_ACTIVATE =
            INTERNAL_V1 + "/tenant-accounts/{accountId}/actions/activate";

    /** 租户账号完成密码恢复后的状态同步路径。 */
    public static final String TENANT_ACCOUNT_COMPLETE_PASSWORD_RESET =
            INTERNAL_V1 + "/tenant-accounts/{accountId}/actions/complete-password-reset";

    /** 禁止实例化 IAM 内部路径常量类。 */
    private IamInternalApiPaths() {
    }
}
