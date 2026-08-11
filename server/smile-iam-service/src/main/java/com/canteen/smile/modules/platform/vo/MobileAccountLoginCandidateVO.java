package com.canteen.smile.modules.platform.vo;

/**
 * Auth 可用于建立租户账号会话的已验证候选快照。
 *
 * @param accountId 租户账号 ID
 * @param tenantId 租户 ID
 * @param tenantName 租户名称
 * @param organizationId 所属机构 ID
 * @param organizationName 所属机构名称
 * @param username 用户名
 * @param displayName 显示名称
 * @param authzVersion 授权版本
 * @param concurrentLoginEnabled 是否允许多设备并发
 * @param maxDevices 最大设备数
 * @param rememberMeEnabled 是否允许记住我
 * @param idleSeconds 普通会话空闲秒数
 * @param absoluteSeconds 普通会话最长秒数
 * @param rememberIdleSeconds 记住我会话空闲秒数
 * @param rememberAbsoluteSeconds 记住我会话最长秒数
 */
public record MobileAccountLoginCandidateVO(
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
