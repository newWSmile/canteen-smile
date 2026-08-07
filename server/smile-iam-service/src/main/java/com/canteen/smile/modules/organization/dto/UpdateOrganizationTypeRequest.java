package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 修改租户机构类型参数。
 *
 * @param name 类型名称
 * @param sortOrder 显示排序值
 * @param version 乐观锁版本
 */
public record UpdateOrganizationTypeRequest(
        @NotBlank @Size(max = 128) String name,
        @Min(0) @Max(100000) int sortOrder,
        @PositiveOrZero long version
) {
}
