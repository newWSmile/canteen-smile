package com.canteen.smile.modules.sms.model;

import java.time.OffsetDateTime;

/**
 * 当前生效的全局短信运行策略。
 *
 * @param challengeTtlSeconds 验证码有效秒数
 * @param resendIntervalSeconds 同手机号重发等待秒数
 * @param maxVerificationAttempts 最大错误校验次数
 * @param mobileHourlyLimit 手机号小时限额
 * @param mobileDailyLimit 手机号每日限额
 * @param ipHourlyLimit IP 小时限额
 * @param ipDailyLimit IP 每日限额
 * @param deviceHourlyLimit 设备小时限额
 * @param deviceDailyLimit 设备每日限额
 * @param plaintextCodeRetentionEnabled 是否保留验证码明文正文
 * @param updatedTime 最后更新时间
 * @param version 乐观锁版本
 */
public record SmsRuntimePolicy(
        int challengeTtlSeconds,
        int resendIntervalSeconds,
        int maxVerificationAttempts,
        int mobileHourlyLimit,
        int mobileDailyLimit,
        int ipHourlyLimit,
        int ipDailyLimit,
        int deviceHourlyLimit,
        int deviceDailyLimit,
        boolean plaintextCodeRetentionEnabled,
        OffsetDateTime updatedTime,
        long version
) {
}
