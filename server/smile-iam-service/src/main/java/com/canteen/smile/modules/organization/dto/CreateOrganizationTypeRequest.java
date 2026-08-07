package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增租户机构类型参数。
 *
 * @param typeCode 租户内永久唯一类型编码
 * @param name 类型名称
 * @param sortOrder 显示排序值
 */
public record CreateOrganizationTypeRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$") String typeCode,
        @NotBlank @Size(max = 128) String name,
        @Min(0) @Max(100000) int sortOrder
) {
}
