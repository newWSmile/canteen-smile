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

    /** 密码信封加密一次性挑战创建路径。 */
    public static final String PASSWORD_ENCRYPTION_CHALLENGES = EXTERNAL_V1 + "/password-encryption/challenges";

    /** 平台恢复码二次验证路径。 */
    public static final String PLATFORM_RECOVERY_LOGIN = EXTERNAL_V1 + "/login/platform-recovery-code";

    /**
     * 经安全评审允许匿名访问的 Auth 精确路径集合。
     * Gateway 与 Servlet 服务必须共同引用，禁止分别维护白名单。
     */
    public static final Set<String> ANONYMOUS_PATHS = Set.of(
            PLATFORM_BOOTSTRAP,
            PASSWORD_ENCRYPTION_CHALLENGES,
            PASSWORD_LOGIN,
            PLATFORM_RECOVERY_LOGIN
    );

    /** 禁止实例化公开路径常量类。 */
    private AuthPublicApiPaths() {
    }
}
