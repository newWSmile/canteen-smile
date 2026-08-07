package com.canteen.smile.modules.auth.vo;

import java.time.OffsetDateTime;

/**
 * 当前平台身份完成再认证后获得的一次性票据。
 *
 * @param reauthTicket 原始再认证票据
 * @param expiresAt 票据绝对失效时间
 */
public record ReauthTicketVO(String reauthTicket, OffsetDateTime expiresAt) {
}
