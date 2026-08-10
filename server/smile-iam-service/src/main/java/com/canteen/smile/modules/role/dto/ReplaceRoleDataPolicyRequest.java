package com.canteen.smile.modules.role.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** @param policies 默认范围与模块覆盖集合 @param version 角色版本 @param reason 修改原因 */
public record ReplaceRoleDataPolicyRequest(
        @NotEmpty @Size(max = 101) List<@Valid RoleDataPolicyItemRequest> policies,
        @PositiveOrZero long version,
        @NotBlank @Size(max = 500) String reason
) {
    /** 保证策略集合不可被外部修改。 */
    public ReplaceRoleDataPolicyRequest {
        policies = policies == null ? List.of() : List.copyOf(policies);
    }
}
