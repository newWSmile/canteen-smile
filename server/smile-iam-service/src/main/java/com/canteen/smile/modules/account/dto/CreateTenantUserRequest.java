package com.canteen.smile.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 创建本机构待激活用户请求。
 *
 * @param username 全平台唯一用户名
 * @param displayName 可选显示名称
 * @param employeeNumber 可选且机构内永久保留的工号
 * @param organizationId 创建后不可修改的所属机构 ID
 * @param roleIds 至少一个可授予自定义角色 ID
 * @param validityMode LONG_TERM 或 FIXED_PERIOD
 * @param effectiveAt 固定周期生效时间
 * @param expiresAt 固定周期到期时间
 * @param reauthTicket 绑定 TENANT_USER_CREATE 的一次性再认证票据
 * @param reason 敏感授权原因
 */
public record CreateTenantUserRequest(
        @NotBlank @Size(max = 128) String username,
        @Size(max = 128) String displayName,
        @Size(max = 64) String employeeNumber,
        @Pattern(regexp = "^[1-9][0-9]*$") String organizationId,
        @NotEmpty List<@Pattern(regexp = "^[1-9][0-9]*$") String> roleIds,
        @Pattern(regexp = "LONG_TERM|FIXED_PERIOD") String validityMode,
        OffsetDateTime effectiveAt,
        OffsetDateTime expiresAt,
        @NotBlank @Size(max = 128) String reauthTicket,
        @NotBlank @Size(max = 500) String reason
) {
}
