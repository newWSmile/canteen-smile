package com.canteen.smile.modules.organization.vo;

/**
 * 租户机构类型允许关系响应。
 *
 * @param id 关系 ID
 * @param parentTypeId 父类型 ID
 * @param childTypeId 子类型 ID
 * @param version 乐观锁版本
 */
public record OrganizationTypeRelationVO(
        String id,
        String parentTypeId,
        String childTypeId,
        long version
) {
}
