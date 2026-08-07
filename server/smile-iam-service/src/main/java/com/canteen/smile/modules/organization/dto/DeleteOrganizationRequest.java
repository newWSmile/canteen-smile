package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 删除完全空白机构参数。
 *
 * @param version 乐观锁版本
 * @param reason 删除原因
 */
public record DeleteOrganizationRequest(
        @PositiveOrZero long version,
        @NotBlank @Size(max = 500) String reason
) {
}
