package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 新增租户机构参数。
 *
 * @param parentId 父机构 ID
 * @param organizationTypeId 机构类型 ID
 * @param businessCode 永久唯一机构业务编码
 * @param name 机构名称
 * @param adminRegionId 可选行政区域 ID
 */
public record CreateOrganizationRequest(
        @Positive long parentId,
        @Positive long organizationTypeId,
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$") String businessCode,
        @NotBlank @Size(max = 200) String name,
        @Positive Long adminRegionId
) {
}
