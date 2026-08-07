package com.canteen.smile.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * IAM 请求创建租户账号凭证容器的内部契约。
 *
 * @param tenantId 租户 ID 字符串
 * @param organizationId 账号所属机构 ID 字符串
 */
public record TenantAccountProvisionRequest(
        @NotBlank @Pattern(regexp = "^[1-9][0-9]*$") String tenantId,
        @NotBlank @Pattern(regexp = "^[1-9][0-9]*$") String organizationId
) {
}
