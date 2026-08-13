package com.canteen.smile.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员为本机构用户发起密码重置请求。
 *
 * @param reauthTicket 当前密码再认证票据
 * @param reason 重置原因
 */
public record TenantUserPasswordResetRequest(
        @NotBlank @Size(max = 128) String reauthTicket,
        @NotBlank @Size(max = 500) String reason
) {
}
