package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 迁移机构参数。
 *
 * @param newParentId 新父机构 ID
 * @param version 乐观锁版本
 * @param reason 迁移原因
 */
public record MoveOrganizationRequest(
        @Positive long newParentId,
        @PositiveOrZero long version,
        @NotBlank @Size(max = 500) String reason
) {
}
