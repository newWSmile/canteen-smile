package com.canteen.smile.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 修改本机构用户非归属资料请求。
 *
 * @param displayName 可选显示名称
 * @param employeeNumber 可选且本机构永久唯一工号
 * @param validityMode LONG_TERM 或 FIXED_PERIOD
 * @param effectiveAt 固定周期生效时间
 * @param expiresAt 固定周期到期时间
 * @param reason 修改原因
 * @param version 用户乐观锁版本
 */
public record UpdateTenantUserRequest(
        @Size(max = 128) String displayName,
        @Size(max = 64) String employeeNumber,
        @Pattern(regexp = "LONG_TERM|FIXED_PERIOD") String validityMode,
        OffsetDateTime effectiveAt,
        OffsetDateTime expiresAt,
        @NotBlank @Size(max = 500) String reason,
        @PositiveOrZero long version
) {
}
