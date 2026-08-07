package com.canteen.smile.internal.client.dto;

/**
 * IAM 请求 Auth 签发租户账号密码恢复票据的内部契约。
 *
 * @param initiatorType 发起身份类型
 * @param initiatorId 发起平台身份 ID
 * @param reauthTicket 五分钟一次性再认证票据
 * @param allowedAction 再认证票据绑定的敏感操作
 */
public record TenantPasswordResetTicketInternalRequest(
        String initiatorType,
        String initiatorId,
        String reauthTicket,
        String allowedAction
) {
}
