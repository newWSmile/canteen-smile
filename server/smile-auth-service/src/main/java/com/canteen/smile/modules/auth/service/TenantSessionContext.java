package com.canteen.smile.modules.auth.service;

/**
 * 已通过密码验证的租户账号会话上下文。
 *
 * @param accountId 账号 ID
 * @param tenantId 租户 ID
 * @param organizationId 机构 ID
 * @param appCode 应用编码
 * @param rememberMe 是否使用记住我
 * @param deviceId 设备稳定标识
 * @param deviceType 设备类型
 * @param deviceName 设备名称
 * @param authzVersion 授权版本
 * @param concurrentLoginEnabled 是否允许多设备并发
 * @param maxDevices 最大设备数
 * @param idleSeconds 空闲秒数
 * @param absoluteSeconds 最长存活秒数
 */
public record TenantSessionContext(
        long accountId,
        long tenantId,
        long organizationId,
        String appCode,
        boolean rememberMe,
        String deviceId,
        String deviceType,
        String deviceName,
        long authzVersion,
        boolean concurrentLoginEnabled,
        int maxDevices,
        int idleSeconds,
        int absoluteSeconds
) {
}
