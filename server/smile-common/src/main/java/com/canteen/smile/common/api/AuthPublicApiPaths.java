package com.canteen.smile.common.api;

import java.util.Set;

/** Auth 服务允许匿名访问且必须经安全评审的外部路径契约。 */
public final class AuthPublicApiPaths {

    /** Auth 外部 v1 根路径。 */
    public static final String EXTERNAL_V1 = "/api/auth/v1";

    /** 首位平台管理员一次性引导路径。 */
    public static final String PLATFORM_BOOTSTRAP = EXTERNAL_V1 + "/platform/bootstrap";

    /** 用户名密码登录路径。 */
    public static final String PASSWORD_LOGIN = EXTERNAL_V1 + "/login/password";

    /** 手机号验证码登录路径。 */
    public static final String SMS_LOGIN = EXTERNAL_V1 + "/login/sms";

    /** 手机号登录多账号选择路径。 */
    public static final String ACCOUNT_SELECTION_LOGIN = EXTERNAL_V1 + "/login/account-selection";

    /** 密码信封加密一次性挑战创建路径。 */
    public static final String PASSWORD_ENCRYPTION_CHALLENGES = EXTERNAL_V1 + "/password-encryption/challenges";

    /** 短信验证码挑战创建路径。 */
    public static final String SMS_CHALLENGES = EXTERNAL_V1 + "/sms/challenges";

    /** 平台恢复码二次验证路径。 */
    public static final String PLATFORM_RECOVERY_LOGIN = EXTERNAL_V1 + "/login/platform-recovery-code";

    /** 一次性账号激活票据资源根路径。 */
    public static final String ACTIVATIONS = EXTERNAL_V1 + "/activations";

    /** 一次性密码重置票据资源根路径。 */
    public static final String PASSWORD_RESETS = EXTERNAL_V1 + "/password-resets";

    /**
     * 经安全评审允许匿名访问的 Auth 精确路径集合。
     * Gateway 与 Servlet 服务必须共同引用，禁止分别维护白名单。
     */
    public static final Set<String> ANONYMOUS_PATHS = Set.of(
            PLATFORM_BOOTSTRAP,
            PASSWORD_ENCRYPTION_CHALLENGES,
            SMS_CHALLENGES,
            PASSWORD_LOGIN,
            SMS_LOGIN,
            ACCOUNT_SELECTION_LOGIN,
            PLATFORM_RECOVERY_LOGIN,
            ACTIVATIONS + "/**",
            PASSWORD_RESETS + "/**"
    );

    /**
     * 判断请求是否属于经过安全评审的匿名 Auth 路径。
     *
     * @param path 请求路径
     * @return 是否允许匿名访问
     */
    public static boolean isAnonymous(String path) {
        return PLATFORM_BOOTSTRAP.equals(path)
                || PASSWORD_ENCRYPTION_CHALLENGES.equals(path)
                || SMS_CHALLENGES.equals(path)
                || PASSWORD_LOGIN.equals(path)
                || SMS_LOGIN.equals(path)
                || ACCOUNT_SELECTION_LOGIN.equals(path)
                || PLATFORM_RECOVERY_LOGIN.equals(path)
                || path.startsWith(ACTIVATIONS + "/")
                || path.startsWith(PASSWORD_RESETS + "/");
    }

    /** 禁止实例化公开路径常量类。 */
    private AuthPublicApiPaths() {
    }
}
