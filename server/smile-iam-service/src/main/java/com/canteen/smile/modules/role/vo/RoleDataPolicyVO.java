package com.canteen.smile.modules.role.vo;

import java.util.List;

/**
 * 角色数据范围响应。
 *
 * @param moduleCode 默认星号或业务模块编码
 * @param moduleName 默认策略或模块名称
 * @param scopeType 范围类型
 * @param organizationIds 指定机构 ID
 */
public record RoleDataPolicyVO(
        String moduleCode,
        String moduleName,
        String scopeType,
        List<String> organizationIds
) {
    /** 保证机构 ID 列表不可变。 */
    public RoleDataPolicyVO {
        organizationIds = List.copyOf(organizationIds);
    }
}
