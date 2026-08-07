package com.canteen.smile.modules.tenant.vo;

import java.time.OffsetDateTime;

/**
 * 平台向租户所有者线下交付的一次性激活信息。
 *
 * @param tenantId 租户 ID
 * @param accountId 所有者账号 ID
 * @param activationTicket 只在本次响应展示的原始票据
 * @param expiresAt 票据失效时间
 */
public record TenantOwnerActivationLinkVO(
        String tenantId,
        String accountId,
        String activationTicket,
        OffsetDateTime expiresAt
) {
}
