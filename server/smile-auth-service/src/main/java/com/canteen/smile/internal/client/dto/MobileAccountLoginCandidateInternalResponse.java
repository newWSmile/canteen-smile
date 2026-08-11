package com.canteen.smile.internal.client.dto;

/** IAM 返回给 Auth 的可登录租户账号与会话策略快照。 */
public record MobileAccountLoginCandidateInternalResponse(
        long accountId,
        long tenantId,
        String tenantName,
        long organizationId,
        String organizationName,
        String username,
        String displayName,
        long authzVersion,
        boolean concurrentLoginEnabled,
        int maxDevices,
        boolean rememberMeEnabled,
        int idleSeconds,
        int absoluteSeconds,
        int rememberIdleSeconds,
        int rememberAbsoluteSeconds
) {
}
