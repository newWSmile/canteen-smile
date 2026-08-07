package com.canteen.smile.internal.client.dto;

import java.time.OffsetDateTime;

/**
 * Auth 签发租户账号密码恢复票据后的内部响应。
 *
 * @param resetTicket 只在本次调用链展示的原始票据
 * @param expiresAt 票据失效时间
 */
public record TenantPasswordResetTicketInternalResponse(
        String resetTicket,
        OffsetDateTime expiresAt
) {
}
