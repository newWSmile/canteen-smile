package com.canteen.smile.modules.account.vo;

import java.time.OffsetDateTime;

/** @param activationTicket 一次性激活票据 @param expiresAt 到期时间 */
public record TenantUserActivationLinkVO(String activationTicket, OffsetDateTime expiresAt) {
}
