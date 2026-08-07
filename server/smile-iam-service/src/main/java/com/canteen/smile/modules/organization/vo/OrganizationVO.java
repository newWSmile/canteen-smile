package com.canteen.smile.modules.organization.vo;

/**
 * 租户机构树节点响应。
 *
 * @param id 机构 ID
 * @param parentId 父机构 ID，根机构为空
 * @param organizationTypeId 机构类型 ID
 * @param typeCode 类型编码
 * @param typeName 类型名称
 * @param businessCode 永久业务编码
 * @param name 机构名称
 * @param adminRegionId 可选行政区域 ID
 * @param ownStatus 自身状态
 * @param effectiveStatus 考虑祖先停用后的实际状态
 * @param pathVersion 路径版本
 * @param hasChildren 是否存在直属子机构
 * @param version 乐观锁版本
 */
public record OrganizationVO(
        String id,
        String parentId,
        String organizationTypeId,
        String typeCode,
        String typeName,
        String businessCode,
        String name,
        String adminRegionId,
        String ownStatus,
        String effectiveStatus,
        long pathVersion,
        boolean hasChildren,
        long version
) {
}
