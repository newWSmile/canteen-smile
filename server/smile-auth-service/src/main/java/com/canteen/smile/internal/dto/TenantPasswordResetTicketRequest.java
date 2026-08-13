package com.canteen.smile.internal.dto;

import com.canteen.smile.modules.auth.model.ReauthAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * IAM 请求 Auth 为租户账号签发密码恢复票据。
 *
 * @param initiatorType 发起人类型，允许平台身份或租户账号
 * @param initiatorId 发起身份 ID 字符串
 * @param reauthTicket 五分钟一次性再认证票据
 * @param allowedAction 再认证票据绑定的敏感操作
 */
public record TenantPasswordResetTicketRequest(
        @NotNull @Pattern(regexp = "PLATFORM_IDENTITY|TENANT_ACCOUNT") String initiatorType,
        @NotNull @Pattern(regexp = "^[1-9][0-9]{0,18}$") String initiatorId,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43,128}$") String reauthTicket,
        @NotNull ReauthAction allowedAction
) {
}
