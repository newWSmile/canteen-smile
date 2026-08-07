package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 平台机构类型模板中的一个类型。
 *
 * @param typeCode 模板内稳定类型编码
 * @param name 类型名称
 * @param sortOrder 显示顺序
 */
public record OrgTypeTemplateItemRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "typeCode 只能使用大写字母、数字和下划线")
        String typeCode,
        @NotBlank @Size(max = 128) String name,
        @Min(0) @Max(100000) int sortOrder
) {
}
