package com.canteen.smile.internal.client.dto;

import java.time.OffsetDateTime;

/**
 * Auth 生成租户账号激活票据后的内部响应。
 *
 * @param activationTicket 只在本次调用链展示的原始票据
 * @param expiresAt 票据失效时间
 */
public record TenantActivationTicketInternalResponse(String activationTicket, OffsetDateTime expiresAt) {
}
