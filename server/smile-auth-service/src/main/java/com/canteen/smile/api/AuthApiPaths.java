package com.canteen.smile.api;

import com.canteen.smile.common.api.AuthPublicApiPaths;

/** Auth 服务外部与内部版本化路径契约。 */
public final class AuthApiPaths {

    /** 经 Gateway 暴露给前端的 Auth v1 根路径。 */
    public static final String EXTERNAL_V1 = AuthPublicApiPaths.EXTERNAL_V1;

    /** 首位平台管理员一次性引导路径。 */
    public static final String PLATFORM_BOOTSTRAP = AuthPublicApiPaths.PLATFORM_BOOTSTRAP;

    /** 用户名密码登录路径。 */
    public static final String PASSWORD_LOGIN = AuthPublicApiPaths.PASSWORD_LOGIN;

    /** 密码信封加密一次性挑战创建路径。 */
    public static final String PASSWORD_ENCRYPTION_CHALLENGES = AuthPublicApiPaths.PASSWORD_ENCRYPTION_CHALLENGES;

    /** 平台恢复码二次验证路径。 */
    public static final String PLATFORM_RECOVERY_LOGIN = AuthPublicApiPaths.PLATFORM_RECOVERY_LOGIN;

    /** 当前登录设备会话查询路径。 */
    public static final String CURRENT_SESSION = EXTERNAL_V1 + "/session";

    /** 当前设备退出登录路径。 */
    public static final String LOGOUT = EXTERNAL_V1 + "/logout";

    /** 禁止实例化 API 路径常量类。 */
    private AuthApiPaths() {
    }
}
