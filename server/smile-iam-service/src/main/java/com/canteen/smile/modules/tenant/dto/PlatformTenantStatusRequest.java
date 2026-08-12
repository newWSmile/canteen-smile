package com.canteen.smile.modules.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 平台执行租户生命周期敏感操作的请求。
 *
 * @param reauthTicket 当前平台身份五分钟一次性再认证票据
 * @param reason 必填操作原因
 * @param version 租户乐观锁版本
 */
public record PlatformTenantStatusRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43,128}$") String reauthTicket,
        @NotBlank @Size(max = 500) String reason,
        @PositiveOrZero long version
) {
}
