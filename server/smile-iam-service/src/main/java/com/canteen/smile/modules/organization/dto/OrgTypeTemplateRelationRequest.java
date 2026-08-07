package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 模板允许的一条机构类型父子关系。
 *
 * @param parentTypeCode 父机构类型编码
 * @param childTypeCode 子机构类型编码
 */
public record OrgTypeTemplateRelationRequest(
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$") String parentTypeCode,
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$") String childTypeCode
) {
}
