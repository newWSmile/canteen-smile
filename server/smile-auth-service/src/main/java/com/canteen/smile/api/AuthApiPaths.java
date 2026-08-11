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

    /** 手机号验证码登录路径。 */
    public static final String SMS_LOGIN = AuthPublicApiPaths.SMS_LOGIN;

    /** 手机号验证码登录多账号选择路径。 */
    public static final String ACCOUNT_SELECTION_LOGIN = AuthPublicApiPaths.ACCOUNT_SELECTION_LOGIN;

    /** 密码信封加密一次性挑战创建路径。 */
    public static final String PASSWORD_ENCRYPTION_CHALLENGES = AuthPublicApiPaths.PASSWORD_ENCRYPTION_CHALLENGES;

    /** 短信验证码挑战创建路径。 */
    public static final String SMS_CHALLENGES = AuthPublicApiPaths.SMS_CHALLENGES;

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

    /** 使用手机号验证码开始租户账号自助找回密码。 */
    public static final String PASSWORD_RESET_SMS_VERIFICATION =
            AuthPublicApiPaths.PASSWORD_RESET_SMS_VERIFICATION;

    /** 自助找回密码时选择手机号绑定的具体租户账号。 */
    public static final String PASSWORD_RESET_SMS_ACCOUNT_SELECTION =
            AuthPublicApiPaths.PASSWORD_RESET_SMS_ACCOUNT_SELECTION;

    /** 当前登录设备会话查询路径。 */
    public static final String CURRENT_SESSION = EXTERNAL_V1 + "/session";

    /** 当前设备退出登录路径。 */
    public static final String LOGOUT = EXTERNAL_V1 + "/logout";

    /** 当前租户账号已验证手机号绑定状态路径。 */
    public static final String MOBILE_BINDING = EXTERNAL_V1 + "/mobile/binding";

    /** 当前租户账号首次绑定手机号挑战创建路径。 */
    public static final String MOBILE_BINDING_CHALLENGES = MOBILE_BINDING + "/challenges";

    /** 当前租户账号首次绑定手机号确认路径。 */
    public static final String MOBILE_BINDING_CONFIRM = MOBILE_BINDING + "/confirm";

    /** 向当前已验证手机号发送敏感操作验证码。 */
    public static final String MOBILE_BINDING_CURRENT_CHALLENGES =
            MOBILE_BINDING + "/current-mobile/challenges";

    /** 使用当前手机号验证码签发换绑或解绑再认证票据。 */
    public static final String MOBILE_BINDING_CURRENT_VERIFICATION =
            MOBILE_BINDING + "/current-mobile/verification";

    /** 向待换绑的新手机号发送验证码。 */
    public static final String MOBILE_BINDING_CHANGE_CHALLENGES =
            MOBILE_BINDING + "/change/challenges";

    /** 原子完成手机号换绑。 */
    public static final String MOBILE_BINDING_CHANGE_CONFIRM =
            MOBILE_BINDING + "/change/confirm";

    /** 原子完成手机号解绑。 */
    public static final String MOBILE_BINDING_UNBIND_CONFIRM =
            MOBILE_BINDING + "/unbind/confirm";

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

    /** IAM 经 HMAC 查询短信投递记录的内部路径。 */
    public static final String INTERNAL_SMS_DELIVERY_SEARCH = INTERNAL_V1 + "/sms-deliveries/search";

    /** IAM 经 HMAC 查询全局短信运行策略的内部路径。 */
    public static final String INTERNAL_SMS_RUNTIME_POLICY = INTERNAL_V1 + "/sms-policy";

    /** IAM 经 HMAC 修改短信验证码与限流策略的内部路径。 */
    public static final String INTERNAL_SMS_RATE_LIMIT_POLICY = INTERNAL_SMS_RUNTIME_POLICY + "/rate-limits";

    /** IAM 经 HMAC 修改短信敏感内容留存策略的内部路径。 */
    public static final String INTERNAL_SMS_SECURITY_POLICY = INTERNAL_SMS_RUNTIME_POLICY + "/security";

    /** 禁止实例化 API 路径常量类。 */
    private AuthApiPaths() {
    }
}
