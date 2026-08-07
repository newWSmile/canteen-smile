package com.canteen.smile.internal.dto;

import java.time.OffsetDateTime;

/**
 * IAM 请求生成激活票据后的内部响应。
 *
 * @param activationTicket 只在本次响应出现的原始一次性票据
 * @param expiresAt 票据失效时间
 */
public record TenantActivationTicketInternalResponse(String activationTicket, OffsetDateTime expiresAt) {
}
