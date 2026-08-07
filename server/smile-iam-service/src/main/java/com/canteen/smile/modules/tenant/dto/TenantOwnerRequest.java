package com.canteen.smile.modules.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 首位租户根机构所有者参数。
 *
 * @param username 全平台永久唯一用户名
 * @param displayName 可选显示名称
 * @param employeeNumber 可选机构内永久唯一工号
 */
public record TenantOwnerRequest(
        @NotBlank @Size(max = 128) String username,
        @Size(max = 128) String displayName,
        @Size(max = 64) String employeeNumber
) {
}
