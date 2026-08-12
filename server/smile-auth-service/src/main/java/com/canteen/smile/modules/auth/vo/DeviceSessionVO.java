package com.canteen.smile.modules.auth.vo;

import java.time.OffsetDateTime;

/**
 * 当前账号可查看的脱敏设备会话。
 *
 * @param sessionId 业务会话 ID
 * @param appCode 登录应用编码
 * @param deviceType 设备类型
 * @param deviceName 设备名称
 * @param loginMethod 登录方式
 * @param loginIpAddress 由网关确认的完整登录客户端 IP
 * @param loginTime 登录时间
 * @param lastActiveTime 最近活动时间
 * @param idleExpiresAt 空闲失效时间
 * @param absoluteExpiresAt 绝对失效时间
 * @param current 是否为当前请求设备
 * @param version 乐观锁版本
 */
public record DeviceSessionVO(
        String sessionId,
        String appCode,
        String deviceType,
        String deviceName,
        String loginMethod,
        String loginIpAddress,
        OffsetDateTime loginTime,
        OffsetDateTime lastActiveTime,
        OffsetDateTime idleExpiresAt,
        OffsetDateTime absoluteExpiresAt,
        boolean current,
        long version
) {
}
