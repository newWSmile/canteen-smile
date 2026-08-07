package com.canteen.smile.modules.auth.vo;

import java.time.OffsetDateTime;

/**
 * 匿名激活页可展示的账号上下文。
 *
 * @param username 用户名
 * @param displayName 显示名称
 * @param tenantName 租户名称
 * @param organizationName 机构名称
 * @param expiresAt 激活票据失效时间
 */
public record ActivationContextVO(
        String username,
        String displayName,
        String tenantName,
        String organizationName,
        OffsetDateTime expiresAt
) {
}
