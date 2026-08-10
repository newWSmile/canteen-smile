package com.canteen.smile.modules.permission.model;

/** IAM 服务已经进入代码契约的集中权限码。 */
public final class IamPermissionCodes {

    /** 平台端查看租户列表和详情。 */
    public static final String PLATFORM_TENANT_VIEW = "platform:tenant:view";

    /** 平台端创建租户。 */
    public static final String PLATFORM_TENANT_CREATE = "platform:tenant:create";

    /** 平台端为租户首位所有者生成激活链接。 */
    public static final String PLATFORM_TENANT_OWNER_ACTIVATE = "platform:tenant-owner:activate";

    /** 重置授权范围内租户账号密码。 */
    public static final String IAM_USER_PASSWORD_RESET = "iam:user:password-reset";

    /** 查看本机构用户。 */
    public static final String IAM_USER_VIEW = "iam:user:view";

    /** 创建本机构用户。 */
    public static final String IAM_USER_CREATE = "iam:user:create";

    /** 修改本机构用户资料。 */
    public static final String IAM_USER_UPDATE = "iam:user:update";

    /** 停用或恢复本机构用户。 */
    public static final String IAM_USER_STATUS = "iam:user:status";

    /** 不可恢复注销本机构用户。 */
    public static final String IAM_USER_CANCEL = "iam:user:cancel";

    /** 替换本机构用户角色集合。 */
    public static final String IAM_USER_ROLE_ASSIGN = "iam:user:role-assign";

    /** 查看授权范围内的租户管理和认证审计。 */
    public static final String IAM_AUDIT_VIEW = "iam:audit:view";

    /** 平台端维护机构类型模板。 */
    public static final String PLATFORM_ORG_TEMPLATE_MANAGE = "platform:org-template:manage";

    /** 平台端维护和发布永久权限资源。 */
    public static final String PLATFORM_PERMISSION_MANAGE = "platform:permission:manage";

    /** 查看平台治理与平台身份认证审计。 */
    public static final String PLATFORM_AUDIT_VIEW = "platform:audit:view";

    /** 查看平台短信发送记录。 */
    public static final String PLATFORM_SMS_DELIVERY_VIEW = "platform:sms-delivery:view";

    /** 查看平台短信验证码和限流设置。 */
    public static final String PLATFORM_SMS_SETTINGS_VIEW = "platform:sms-settings:view";

    /** 修改平台短信验证码和限流设置。 */
    public static final String PLATFORM_SMS_SETTINGS_UPDATE = "platform:sms-settings:update";

    /** 查看平台短信安全设置。 */
    public static final String PLATFORM_SMS_SECURITY_VIEW = "platform:sms-security:view";

    /** 修改平台短信安全设置。 */
    public static final String PLATFORM_SMS_SECURITY_UPDATE = "platform:sms-security:update";

    /** 查看本租户机构类型。 */
    public static final String IAM_ORG_TYPE_VIEW = "iam:org-type:view";

    /** 维护本租户机构类型和允许关系。 */
    public static final String IAM_ORG_TYPE_MANAGE = "iam:org-type:manage";

    /** 查看授权范围内机构。 */
    public static final String IAM_ORG_VIEW = "iam:org:view";

    /** 新增授权范围内机构。 */
    public static final String IAM_ORG_CREATE = "iam:org:create";

    /** 修改授权范围内机构。 */
    public static final String IAM_ORG_UPDATE = "iam:org:update";

    /** 迁移授权范围内机构。 */
    public static final String IAM_ORG_MOVE = "iam:org:move";

    /** 停用或恢复授权范围内机构。 */
    public static final String IAM_ORG_STATUS = "iam:org:status";

    /** 删除从未使用的空白机构。 */
    public static final String IAM_ORG_DELETE = "iam:org:delete";

    /** 查看本机构角色。 */
    public static final String IAM_ROLE_VIEW = "iam:role:view";

    /** 创建本机构角色。 */
    public static final String IAM_ROLE_CREATE = "iam:role:create";

    /** 修改本机构角色资料。 */
    public static final String IAM_ROLE_UPDATE = "iam:role:update";

    /** 停用或恢复本机构角色。 */
    public static final String IAM_ROLE_STATUS = "iam:role:status";

    /** 删除本机构自定义角色。 */
    public static final String IAM_ROLE_DELETE = "iam:role:delete";

    /** 分配本机构角色功能权限。 */
    public static final String IAM_ROLE_GRANT = "iam:role:grant";

    /** 配置本机构角色数据范围。 */
    public static final String IAM_ROLE_DATA_SCOPE = "iam:role:data-scope";

    /** 禁止实例化权限码常量类。 */
    private IamPermissionCodes() {
    }
}
