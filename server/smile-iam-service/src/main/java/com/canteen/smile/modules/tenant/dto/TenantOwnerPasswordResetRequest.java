package com.canteen.smile.modules.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 平台为租户所有者生成密码恢复链接的敏感操作参数。
 *
 * @param reauthTicket 当前平台身份五分钟一次性再认证票据
 * @param reason 管理员填写的操作原因
 */
public record TenantOwnerPasswordResetRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43,128}$") String reauthTicket,
        @NotBlank @Size(max = 500) String reason
) {
}
