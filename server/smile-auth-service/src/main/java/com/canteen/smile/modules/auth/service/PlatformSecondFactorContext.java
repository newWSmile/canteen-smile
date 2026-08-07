package com.canteen.smile.modules.auth.service;

/**
 * 平台登录或风险二次验证使用的安全上下文，不包含密码、恢复码或 Token。
 *
 * @param platformIdentityId 平台身份 ID
 * @param username 当前用户名
 * @param appCode 应用编码
 * @param rememberMe 是否申请记住我会话
 * @param deviceId 客户端稳定设备标识
 * @param deviceType 设备类型
 * @param deviceName 设备名称
 * @param authzVersion IAM 授权版本
 */
public record PlatformSecondFactorContext(
        long platformIdentityId,
        String username,
        String appCode,
        boolean rememberMe,
        String deviceId,
        String deviceType,
        String deviceName,
        long authzVersion
) {
}
