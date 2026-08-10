package com.canteen.smile.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建本机构角色请求。
 *
 * @param name 角色名称
 * @param description 可选说明
 * @param defaultScopeType 默认数据范围
 * @param specifiedOrganizationIds 指定机构范围使用的机构 ID
 */
public record CreateRoleRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 500) String description,
        @NotBlank @Pattern(regexp = "SELF|CURRENT_ORG|CURRENT_ORG_AND_DESCENDANTS|SPECIFIED_ORGS|SPECIFIED_ORGS_AND_DESCENDANTS|TENANT_ALL")
        String defaultScopeType,
        @Size(max = 500) List<@Pattern(regexp = "[1-9][0-9]*") String> specifiedOrganizationIds
) {
    /** 保证集合不为 null。 */
    public CreateRoleRequest {
        specifiedOrganizationIds = specifiedOrganizationIds == null ? List.of() : List.copyOf(specifiedOrganizationIds);
    }
}
