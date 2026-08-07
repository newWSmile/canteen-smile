package com.canteen.smile.internal.dto;

/**
 * 租户账号凭证初始化内部响应。
 *
 * @param accountId 租户账号 ID 字符串
 * @param credentialStatus Auth 凭证状态
 */
public record TenantAccountProvisionResponse(String accountId, String credentialStatus) {
}
