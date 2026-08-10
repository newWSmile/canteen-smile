package com.canteen.smile.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 敏感替换用户角色集合请求。
 *
 * @param roleIds 至少一个可授予自定义角色 ID
 * @param reauthTicket 绑定 TENANT_USER_ROLE_ASSIGN 的一次性票据
 * @param reason 操作原因
 * @param version 用户乐观锁版本
 */
public record ReplaceTenantUserRolesRequest(
        @NotEmpty List<@Pattern(regexp = "^[1-9][0-9]*$") String> roleIds,
        @NotBlank @Size(max = 128) String reauthTicket,
        @NotBlank @Size(max = 500) String reason,
        @PositiveOrZero long version
) {
}
