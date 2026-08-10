package com.canteen.smile.internal.client.dto;

/** IAM 发给 Auth 的短信限流策略更新契约。 */
public record SmsRateLimitPolicyUpdateInternalRequest(
        int challengeTtlSeconds,
        int resendIntervalSeconds,
        int maxVerificationAttempts,
        int mobileHourlyLimit,
        int mobileDailyLimit,
        int ipHourlyLimit,
        int ipDailyLimit,
        int deviceHourlyLimit,
        int deviceDailyLimit,
        long version,
        long actorId,
        String reauthTicket
) {
}
