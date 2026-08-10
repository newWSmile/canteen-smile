package com.canteen.smile.api;

import com.canteen.smile.common.api.AuthPublicApiPaths;

/** Auth 服务外部与内部版本化路径契约。 */
public final class AuthApiPaths {

    /** 仅允许可信内部网络与 HMAC 调用的 Auth v1 根路径。 */
    public static final String INTERNAL_V1 = "/internal/auth/v1";

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

    /** 账号激活上下文和完成动作路径。 */
    public static final String ACTIVATION_CONTEXT = AuthPublicApiPaths.ACTIVATIONS + "/{ticket}/context";

    /** 使用一次性票据设置初始密码并完成账号激活。 */
    public static final String ACTIVATION_COMPLETE = AuthPublicApiPaths.ACTIVATIONS + "/{ticket}/complete";

    /** 当前登录平台身份使用密码完成敏感操作再认证。 */
    public static final String PASSWORD_REAUTH = EXTERNAL_V1 + "/reauth/password";

    /** 一次性密码重置票据上下文路径。 */
    public static final String PASSWORD_RESET_CONTEXT =
            AuthPublicApiPaths.PASSWORD_RESETS + "/{ticket}/context";

    /** 使用一次性票据设置新密码。 */
    public static final String PASSWORD_RESET_COMPLETE =
            AuthPublicApiPaths.PASSWORD_RESETS + "/{ticket}/complete";

    /** 当前登录设备会话查询路径。 */
    public static final String CURRENT_SESSION = EXTERNAL_V1 + "/session";

    /** 当前设备退出登录路径。 */
    public static final String LOGOUT = EXTERNAL_V1 + "/logout";

    /** IAM 调用的租户账号凭证初始化内部接口。 */
    public static final String INTERNAL_TENANT_ACCOUNT_PROVISION =
            "/internal/auth/v1/tenant-accounts/{accountId}/provision";

    /** IAM 请求生成租户账号一次性激活票据。 */
    public static final String INTERNAL_TENANT_ACCOUNT_ACTIVATION_TICKETS =
            "/internal/auth/v1/tenant-accounts/{accountId}/activation-tickets";

    /** IAM 为租户账号请求生成一次性密码重置票据。 */
    public static final String INTERNAL_TENANT_ACCOUNT_PASSWORD_RESET_TICKETS =
            "/internal/auth/v1/tenant-accounts/{accountId}/password-reset-tickets";

    /** IAM 原子消费敏感操作再认证票据的内部接口。 */
    public static final String INTERNAL_REAUTH_TICKET_CONSUME =
            "/internal/auth/v1/reauth-tickets/actions/consume";

    /** IAM Outbox 安全事件幂等消费路径。 */
    public static final String INTERNAL_SECURITY_EVENTS = INTERNAL_V1 + "/security-events";

    /** IAM 经 HMAC 查询 Auth 自有认证审计日志的内部路径。 */
    public static final String INTERNAL_AUDIT_LOG_SEARCH = INTERNAL_V1 + "/audit-logs/search";

    /** 禁止实例化 API 路径常量类。 */
    private AuthApiPaths() {
    }
}
