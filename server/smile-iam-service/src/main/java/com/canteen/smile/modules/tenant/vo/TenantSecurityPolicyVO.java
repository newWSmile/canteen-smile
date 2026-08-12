package com.canteen.smile.modules.tenant.vo;

/**
 * 当前租户安全策略视图。
 *
 * @param tenantId 租户 ID
 * @param tenantName 租户名称
 * @param concurrentLoginEnabled 是否允许多设备同时登录
 * @param maxDevices 最大有效设备数
 * @param rememberMeEnabled 是否允许记住我
 * @param idleSeconds 普通会话空闲超时秒数
 * @param absoluteSeconds 普通会话最长存活秒数
 * @param rememberIdleSeconds 记住我会话空闲超时秒数
 * @param rememberAbsoluteSeconds 记住我会话最长存活秒数
 * @param passwordExpiryEnabled 是否启用密码定期到期
 * @param passwordExpiryDays 密码有效天数
 * @param auditRetentionDays 审计保留天数
 * @param securityVersion 租户安全版本
 * @param version 策略乐观锁版本
 */
public record TenantSecurityPolicyVO(
        String tenantId,
        String tenantName,
        boolean concurrentLoginEnabled,
        int maxDevices,
        boolean rememberMeEnabled,
        int idleSeconds,
        int absoluteSeconds,
        int rememberIdleSeconds,
        int rememberAbsoluteSeconds,
        boolean passwordExpiryEnabled,
        Integer passwordExpiryDays,
        int auditRetentionDays,
        long securityVersion,
        long version
) {
}
