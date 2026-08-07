package com.canteen.smile.internal.dto;

import java.time.OffsetDateTime;

/**
 * 只在 IAM 当前调用响应中展示一次的密码恢复票据。
 *
 * @param resetTicket 原始一次性票据
 * @param expiresAt 票据绝对失效时间
 */
public record TenantPasswordResetTicketResponse(String resetTicket, OffsetDateTime expiresAt) {
}
