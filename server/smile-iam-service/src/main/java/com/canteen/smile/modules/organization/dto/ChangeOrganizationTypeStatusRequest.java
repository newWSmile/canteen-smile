package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 修改租户机构类型状态参数。
 *
 * @param status 目标状态
 * @param version 乐观锁版本
 */
public record ChangeOrganizationTypeStatusRequest(
        @NotNull @Pattern(regexp = "ACTIVE|DISABLED") String status,
        @PositiveOrZero long version
) {
}
