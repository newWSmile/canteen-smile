package com.canteen.smile.modules.account.service;

/**
 * 已完成数据库最终校验的租户操作人上下文。
 *
 * @param accountId 账号 ID
 * @param tenantId 租户 ID
 * @param tenantName 租户名称
 * @param organizationId 所属机构 ID
 * @param organizationName 所属机构名称
 * @param rootOrganizationId 根机构 ID
 * @param username 用户名
 * @param displayName 显示名称
 * @param organizationOwner 是否当前所属机构所有者
 * @param rootOwner 是否租户根机构所有者
 */
public record TenantActorContext(
        long accountId,
        long tenantId,
        String tenantName,
        long organizationId,
        String organizationName,
        long rootOrganizationId,
        String username,
        String displayName,
        boolean organizationOwner,
        boolean rootOwner
) {
}
