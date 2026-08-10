package com.canteen.smile.modules.role.vo;

import java.util.List;

/**
 * 当前操作者可授予上限。
 *
 * @param organizationId 只能管理的当前机构 ID
 * @param rootOwner 是否根机构所有者
 * @param permissionIds 可授予权限资源 ID
 * @param scopeTypes 可配置数据范围类型
 */
public record GrantBoundaryVO(
        String organizationId,
        boolean rootOwner,
        List<String> permissionIds,
        List<String> scopeTypes
) {
    /** 保证响应集合不可变。 */
    public GrantBoundaryVO {
        permissionIds = List.copyOf(permissionIds);
        scopeTypes = List.copyOf(scopeTypes);
    }
}
