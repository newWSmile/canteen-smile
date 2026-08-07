package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.Positive;

/**
 * 一个允许的机构类型父子关系。
 *
 * @param parentTypeId 父类型 ID
 * @param childTypeId 子类型 ID
 */
public record OrganizationTypeRelationRequest(
        @Positive long parentTypeId,
        @Positive long childTypeId
) {
}
