package com.canteen.smile.internal.client.dto;

/**
 * IAM 返回给 Auth 的租户账号激活上下文。
 *
 * @param accountId 账号 ID
 * @param tenantId 租户 ID
 * @param organizationId 机构 ID
 * @param username 用户名
 * @param displayName 显示名称
 * @param tenantName 租户名称
 * @param organizationName 机构名称
 * @param status 账号状态
 */
public record TenantAccountActivationContextInternalResponse(
        String accountId,
        String tenantId,
        String organizationId,
        String username,
        String displayName,
        String tenantName,
        String organizationName,
        String status
) {
}
