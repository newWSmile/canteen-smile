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

    /** 平台端维护机构类型模板。 */
    public static final String PLATFORM_ORG_TEMPLATE_MANAGE = "platform:org-template:manage";

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

    /** 禁止实例化权限码常量类。 */
    private IamPermissionCodes() {
    }
}
