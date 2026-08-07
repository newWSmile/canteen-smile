package com.canteen.smile.modules.auth.vo;

import java.time.OffsetDateTime;

/**
 * 当前设备的认证会话响应；bigint 身份 ID 使用字符串避免前端精度丢失。
 *
 * @param tokenName Token 请求头名称
 * @param tokenValue 只返回给当前设备的 Token
 * @param sessionId 设备会话 ID
 * @param appCode 前端应用编码
 * @param identityType 认证主体类型
 * @param accountId 主体十进制字符串 ID；平台端表示平台身份 ID
 * @param tenantId 租户 ID，平台身份为空
 * @param organizationId 机构 ID，平台身份为空
 * @param idleExpiresAt 空闲失效时间
 * @param absoluteExpiresAt 绝对失效时间
 */
public record SessionVO(
        String tokenName,
        String tokenValue,
        String sessionId,
        String appCode,
        String identityType,
        String accountId,
        String tenantId,
        String organizationId,
        OffsetDateTime idleExpiresAt,
        OffsetDateTime absoluteExpiresAt
) {
}
