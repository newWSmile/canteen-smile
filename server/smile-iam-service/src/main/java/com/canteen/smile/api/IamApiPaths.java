package com.canteen.smile.api;

/** IAM 服务外部与内部版本化路径契约。 */
public final class IamApiPaths {

    /** 经 Gateway 暴露给前端的 IAM v1 根路径。 */
    public static final String EXTERNAL_V1 = "/api/iam/v1";

    /** 仅供受信任服务通过 HMAC 调用的 IAM v1 根路径。 */
    public static final String INTERNAL_V1 = "/internal/iam/v1";

    /** Auth 使用的用户名登录解析路径。 */
    public static final String USERNAME_LOGIN_RESOLUTION = INTERNAL_V1 + "/login-resolutions/username";

    /** Auth 查询租户账号激活上下文的内部路径。 */
    public static final String TENANT_ACCOUNT_ACTIVATION_CONTEXT =
            INTERNAL_V1 + "/tenant-accounts/{accountId}/activation-context";

    /** Auth 完成凭证激活后同步 IAM 账号状态的内部路径。 */
    public static final String TENANT_ACCOUNT_ACTIVATE =
            INTERNAL_V1 + "/tenant-accounts/{accountId}/actions/activate";

    /** Auth 完成密码恢复后同步 IAM 账号状态的内部路径。 */
    public static final String TENANT_ACCOUNT_COMPLETE_PASSWORD_RESET =
            INTERNAL_V1 + "/tenant-accounts/{accountId}/actions/complete-password-reset";

    /** 平台端租户资源路径。 */
    public static final String PLATFORM_TENANTS = EXTERNAL_V1 + "/platform/tenants";

    /** 平台为租户根机构所有者生成激活链接的动作路径。 */
    public static final String PLATFORM_TENANT_OWNER_ACTIVATION_LINKS =
            "/{tenantId}/owner/activation-links";

    /** 平台为租户根机构所有者生成密码恢复链接的动作路径。 */
    public static final String PLATFORM_TENANT_OWNER_PASSWORD_RESET_LINKS =
            "/{tenantId}/owner/password-reset-links";

    /** 平台机构类型模板资源路径。 */
    public static final String PLATFORM_ORG_TYPE_TEMPLATES = EXTERNAL_V1 + "/platform/org-type-templates";

    /** 租户管理端当前身份与租户上下文。 */
    public static final String TENANT_CONTEXT = EXTERNAL_V1 + "/tenant/context";

    /** 租户独立机构类型资源。 */
    public static final String TENANT_ORGANIZATION_TYPES = EXTERNAL_V1 + "/tenant/organization-types";

    /** 租户机构类型允许关系资源。 */
    public static final String TENANT_ORGANIZATION_TYPE_RELATIONS =
            EXTERNAL_V1 + "/tenant/organization-type-relations";

    /** 租户机构树资源。 */
    public static final String TENANT_ORGANIZATIONS = EXTERNAL_V1 + "/tenant/organizations";

    /** 禁止实例化 API 路径常量类。 */
    private IamApiPaths() {
    }
}
