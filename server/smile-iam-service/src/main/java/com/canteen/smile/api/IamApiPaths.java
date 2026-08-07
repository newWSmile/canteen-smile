package com.canteen.smile.api;

/** IAM 服务外部与内部版本化路径契约。 */
public final class IamApiPaths {

    /** 经 Gateway 暴露给前端的 IAM v1 根路径。 */
    public static final String EXTERNAL_V1 = "/api/iam/v1";

    /** 仅供受信任服务通过 HMAC 调用的 IAM v1 根路径。 */
    public static final String INTERNAL_V1 = "/internal/iam/v1";

    /** Auth 使用的用户名登录解析路径。 */
    public static final String USERNAME_LOGIN_RESOLUTION = INTERNAL_V1 + "/login-resolutions/username";

    /** 平台端租户资源路径。 */
    public static final String PLATFORM_TENANTS = EXTERNAL_V1 + "/platform/tenants";

    /** 禁止实例化 API 路径常量类。 */
    private IamApiPaths() {
    }
}
