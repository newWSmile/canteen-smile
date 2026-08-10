package com.canteen.smile.modules.role.vo;

/**
 * 可分配权限树节点。
 *
 * @param id 权限资源 ID
 * @param parentId 父节点 ID
 * @param permissionCode 权限码
 * @param name 名称
 * @param resourceType 资源类型
 * @param appCode 应用编码
 * @param featureCode 功能编码
 * @param sortOrder 排序值
 * @param granted 当前角色是否已拥有
 */
public record RolePermissionVO(
        String id,
        String parentId,
        String permissionCode,
        String name,
        String resourceType,
        String appCode,
        String featureCode,
        int sortOrder,
        boolean granted
) {
}
