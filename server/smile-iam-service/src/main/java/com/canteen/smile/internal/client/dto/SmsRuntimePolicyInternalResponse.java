package com.canteen.smile.internal.client.dto;

import java.time.OffsetDateTime;

/** Auth 返回 IAM 的全局短信运行策略契约。 */
public record SmsRuntimePolicyInternalResponse(
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
