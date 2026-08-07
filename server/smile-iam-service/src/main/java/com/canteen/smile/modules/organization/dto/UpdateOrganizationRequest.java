package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 修改租户机构资料参数。
 *
 * @param organizationTypeId 机构类型 ID
 * @param name 机构名称
 * @param adminRegionId 可选行政区域 ID
 * @param version 乐观锁版本
 */
public record UpdateOrganizationRequest(
        @Positive long organizationTypeId,
        @NotBlank @Size(max = 200) String name,
        @Positive Long adminRegionId,
        @PositiveOrZero long version
) {
}
