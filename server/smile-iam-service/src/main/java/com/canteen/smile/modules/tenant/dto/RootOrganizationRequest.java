package com.canteen.smile.modules.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建租户时的根机构参数。
 *
 * @param typeCode 已发布模板中的机构类型编码
 * @param businessCode 租户内永久保留的机构业务编码
 * @param name 根机构名称
 * @param adminRegionId 可选行政区域 ID 字符串
 */
public record RootOrganizationRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Z][A-Z0-9_]*$") String typeCode,
        @NotBlank @Size(max = 64) String businessCode,
        @NotBlank @Size(max = 200) String name,
        @Pattern(regexp = "^[1-9][0-9]*$") String adminRegionId
) {
}
