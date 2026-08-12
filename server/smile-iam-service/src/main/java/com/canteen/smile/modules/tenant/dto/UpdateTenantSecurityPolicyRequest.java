package com.canteen.smile.modules.tenant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 租户根机构所有者修改安全策略的命令。
 *
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
 * @param version 当前策略乐观锁版本
 * @param reauthTicket 绑定本动作的一次性再认证票据
 * @param reason 修改原因
 */
public record UpdateTenantSecurityPolicyRequest(
        @NotNull Boolean concurrentLoginEnabled,
        @NotNull @Min(1) @Max(100) Integer maxDevices,
        @NotNull Boolean rememberMeEnabled,
        @NotNull @Min(60) Integer idleSeconds,
        @NotNull @Min(60) Integer absoluteSeconds,
        @NotNull @Min(60) Integer rememberIdleSeconds,
        @NotNull @Min(60) Integer rememberAbsoluteSeconds,
        @NotNull Boolean passwordExpiryEnabled,
        @Min(1) @Max(3650) Integer passwordExpiryDays,
        @NotNull @Min(180) Integer auditRetentionDays,
        @NotNull @Min(0) Long version,
        @NotBlank @Size(max = 256) String reauthTicket,
        @NotBlank @Size(max = 500) String reason
) {
}
