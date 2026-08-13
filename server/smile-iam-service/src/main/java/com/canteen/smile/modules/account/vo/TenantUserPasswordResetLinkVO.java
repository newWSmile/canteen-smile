package com.canteen.smile.modules.account.vo;

import java.time.OffsetDateTime;

/**
 * 管理员生成的一次性密码重置链接票据。
 *
 * @param resetTicket 一次性重置票据
 * @param expiresAt 过期时间
 */
public record TenantUserPasswordResetLinkVO(String resetTicket, OffsetDateTime expiresAt) {
}
