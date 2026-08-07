package com.canteen.smile.internal.client.dto;

/**
 * IAM 发往 Auth 的租户账号凭证初始化契约。
 *
 * @param tenantId 租户 ID 字符串
 * @param organizationId 账号所属机构 ID 字符串
 */
public record TenantAccountProvisionInternalRequest(String tenantId, String organizationId) {
}
