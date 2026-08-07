package com.canteen.smile.modules.auth.vo;

import java.time.OffsetDateTime;

/**
 * 一次性密码恢复页面展示的非敏感账号上下文。
 *
 * @param username 用户名
 * @param displayName 显示名称
 * @param tenantName 租户名称
 * @param organizationName 机构名称
 * @param expiresAt 恢复链接绝对失效时间
 */
public record PasswordResetContextVO(
        String username,
        String displayName,
        String tenantName,
        String organizationName,
        OffsetDateTime expiresAt
) {
}
