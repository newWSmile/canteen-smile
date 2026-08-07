package com.canteen.smile.modules.tenant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 平台创建租户的完整初始化命令。
 *
 * @param tenantCode 永久唯一租户编码
 * @param name 租户名称
 * @param templateVersion 已发布机构类型模板版本
 * @param rootOrganization 根机构参数
 * @param owner 首位根机构所有者参数
 * @param securityPolicy 租户安全策略
 */
public record CreateTenantRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Z][A-Z0-9_-]*$") String tenantCode,
        @NotBlank @Size(max = 200) String name,
        @NotNull @Positive Long templateVersion,
        @NotNull @Valid RootOrganizationRequest rootOrganization,
        @NotNull @Valid TenantOwnerRequest owner,
        @NotNull @Valid TenantSecurityPolicyRequest securityPolicy
) {
}
