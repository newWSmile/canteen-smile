package com.canteen.smile.internal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** IAM 修改短信验证码和多维限流策略的内部签名请求。 */
public record SmsRateLimitPolicyUpdateInternalRequest(
        @Min(60) @Max(900) int challengeTtlSeconds,
        @Min(30) @Max(600) int resendIntervalSeconds,
        @Min(1) @Max(5) int maxVerificationAttempts,
        @Min(1) @Max(100) int mobileHourlyLimit,
        @Min(1) @Max(500) int mobileDailyLimit,
        @Min(1) @Max(1000) int ipHourlyLimit,
        @Min(1) @Max(5000) int ipDailyLimit,
        @Min(1) @Max(500) int deviceHourlyLimit,
        @Min(1) @Max(2000) int deviceDailyLimit,
        @Min(0) long version,
        @Positive long actorId,
        @NotBlank @Size(max = 128) String reauthTicket
) {
}
