package com.canteen.smile.modules.sms.vo;

import java.time.OffsetDateTime;

/** 平台端短信限流和安全设置响应。 */
public record SmsRuntimePolicyVO(
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
