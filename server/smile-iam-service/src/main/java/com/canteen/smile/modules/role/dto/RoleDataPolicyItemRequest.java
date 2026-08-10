package com.canteen.smile.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 单条角色数据范围策略。
 *
 * @param moduleCode 星号为默认范围，其它值为已发布模块编码
 * @param scopeType 数据范围类型
 * @param organizationIds 指定机构集合
 */
public record RoleDataPolicyItemRequest(
        @NotBlank @Size(max = 128) String moduleCode,
        @NotBlank @Pattern(regexp = "SELF|CURRENT_ORG|CURRENT_ORG_AND_DESCENDANTS|SPECIFIED_ORGS|SPECIFIED_ORGS_AND_DESCENDANTS|TENANT_ALL")
        String scopeType,
        @Size(max = 500) List<@Pattern(regexp = "[1-9][0-9]*") String> organizationIds
) {
    /** 保证集合不为 null。 */
    public RoleDataPolicyItemRequest {
        organizationIds = organizationIds == null ? List.of() : List.copyOf(organizationIds);
    }
}
