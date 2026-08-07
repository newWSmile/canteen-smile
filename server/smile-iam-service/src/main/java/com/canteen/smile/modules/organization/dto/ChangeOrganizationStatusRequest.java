package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 修改机构自身状态参数。
 *
 * @param status ACTIVE 或 DISABLED
 * @param version 乐观锁版本
 * @param reason 状态变更原因
 */
public record ChangeOrganizationStatusRequest(
        @NotNull @Pattern(regexp = "ACTIVE|DISABLED") String status,
        @PositiveOrZero long version,
        @NotBlank @Size(max = 500) String reason
) {
}
