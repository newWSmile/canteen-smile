package com.canteen.smile.modules.organization.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 整版替换租户机构类型允许关系。
 *
 * @param relations 完整关系集合
 */
public record ReplaceOrganizationTypeRelationsRequest(
        @NotNull @Size(max = 500) List<@Valid OrganizationTypeRelationRequest> relations
) {
}
