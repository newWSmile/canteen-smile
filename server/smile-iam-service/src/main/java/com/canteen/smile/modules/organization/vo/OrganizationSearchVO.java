package com.canteen.smile.modules.organization.vo;

/**
 * 机构关键词搜索结果。
 *
 * @param id 机构 ID
 * @param parentId 父机构 ID
 * @param organizationTypeId 类型 ID
 * @param typeName 类型名称
 * @param businessCode 业务编码
 * @param name 机构名称
 * @param effectiveStatus 实际状态
 * @param breadcrumb 从根机构到当前机构的路径
 */
public record OrganizationSearchVO(
        String id,
        String parentId,
        String organizationTypeId,
        String typeName,
        String businessCode,
        String name,
        String effectiveStatus,
        String breadcrumb
) {
}
