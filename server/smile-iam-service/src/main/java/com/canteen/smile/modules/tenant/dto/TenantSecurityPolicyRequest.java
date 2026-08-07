package com.canteen.smile.modules.tenant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 租户登录与审计安全策略参数。
 *
 * @param concurrentLoginEnabled 是否允许多设备同时登录
 * @param maxDevices 最大有效设备数
 * @param rememberMeEnabled 是否允许记住我
 * @param idleSeconds 普通会话空闲超时秒数
 * @param absoluteSeconds 普通会话最长存活秒数
 * @param rememberIdleSeconds 记住我会话空闲超时秒数
 * @param rememberAbsoluteSeconds 记住我会话最长存活秒数
 * @param passwordExpiryEnabled 是否启用密码到期
 * @param passwordExpiryDays 密码有效天数
 * @param auditRetentionDays 审计保留天数
 */
public record TenantSecurityPolicyRequest(
        @NotNull Boolean concurrentLoginEnabled,
        @NotNull @Min(1) @Max(100) Integer maxDevices,
        @NotNull Boolean rememberMeEnabled,
        @NotNull @Min(60) Integer idleSeconds,
        @NotNull @Min(60) Integer absoluteSeconds,
        @NotNull @Min(60) Integer rememberIdleSeconds,
        @NotNull @Min(60) Integer rememberAbsoluteSeconds,
        @NotNull Boolean passwordExpiryEnabled,
        @Min(1) @Max(3650) Integer passwordExpiryDays,
        @NotNull @Min(180) Integer auditRetentionDays
) {
}
