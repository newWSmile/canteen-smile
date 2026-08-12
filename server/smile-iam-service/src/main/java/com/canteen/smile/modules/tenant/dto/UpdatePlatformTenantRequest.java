package com.canteen.smile.modules.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 平台修改租户可变基础资料的请求。
 *
 * @param name 租户显示名称
 * @param version 租户乐观锁版本
 */
public record UpdatePlatformTenantRequest(
        @NotBlank @Size(max = 200) String name,
        @PositiveOrZero long version
) {
}
