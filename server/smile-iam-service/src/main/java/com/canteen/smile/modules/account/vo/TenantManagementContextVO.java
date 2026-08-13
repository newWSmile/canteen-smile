package com.canteen.smile.modules.account.vo;

import java.util.List;

/**
 * 租户管理端启动所需的真实身份和权限上下文。
 *
 * @param accountId 当前账号 ID
 * @param username 当前用户名
 * @param displayName 当前显示名称
 * @param tenantId 当前租户 ID
 * @param tenantName 当前租户名称
 * @param organizationId 当前所属机构 ID
 * @param organizationName 当前所属机构名称
 * @param rootOrganizationId 租户根机构 ID
 * @param organizationOwner 是否当前所属机构所有者
 * @param rootOwner 是否根机构所有者
 * @param permissions 当前后端最终权限码
 * @param hiddenMenuPermissionCodes 租户配置和个人偏好共同决定的隐藏菜单权限码
 */
public record TenantManagementContextVO(
        String accountId,
        String username,
        String displayName,
        String tenantId,
        String tenantName,
        String organizationId,
        String organizationName,
        String rootOrganizationId,
        boolean organizationOwner,
        boolean rootOwner,
        List<String> permissions,
        List<String> hiddenMenuPermissionCodes
) {
    /** 对权限集合创建不可变副本。 */
    public TenantManagementContextVO {
        permissions = List.copyOf(permissions);
        hiddenMenuPermissionCodes = List.copyOf(hiddenMenuPermissionCodes);
    }
}
