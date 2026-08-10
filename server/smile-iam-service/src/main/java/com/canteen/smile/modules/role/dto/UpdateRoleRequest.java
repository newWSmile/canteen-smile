package com.canteen.smile.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** @param name 角色名称 @param description 可选说明 @param version 乐观锁版本 */
public record UpdateRoleRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 500) String description,
        @PositiveOrZero long version
) {
}
