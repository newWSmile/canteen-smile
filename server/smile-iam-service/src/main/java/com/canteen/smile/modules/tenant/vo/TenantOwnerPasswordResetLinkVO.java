package com.canteen.smile.modules.tenant.vo;

import java.time.OffsetDateTime;

/**
 * 平台向租户所有者线下交付的一次性密码恢复信息。
 *
 * @param tenantId 租户 ID
 * @param accountId 所有者账号 ID
 * @param resetTicket 只在本次响应展示的原始恢复票据
 * @param expiresAt 票据失效时间
 */
public record TenantOwnerPasswordResetLinkVO(
        String tenantId,
        String accountId,
        String resetTicket,
        OffsetDateTime expiresAt
) {
}
