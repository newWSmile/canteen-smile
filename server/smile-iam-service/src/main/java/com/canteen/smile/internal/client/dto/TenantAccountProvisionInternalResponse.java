package com.canteen.smile.internal.client.dto;

/**
 * Auth 返回的租户账号凭证初始化结果。
 *
 * @param accountId 账号 ID 字符串
 * @param credentialStatus Auth 凭证状态
 */
public record TenantAccountProvisionInternalResponse(String accountId, String credentialStatus) {
}
