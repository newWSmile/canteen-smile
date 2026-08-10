package com.canteen.smile.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** @param version 乐观锁版本 @param reason 必填操作原因 */
public record RoleStatusRequest(
        @PositiveOrZero long version,
        @NotBlank @Size(max = 500) String reason
) {
}
