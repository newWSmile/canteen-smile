package com.canteen.smile.modules.sms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 平台修改短信验证码与多维限流设置的请求。 */
public record SmsRateLimitSettingsUpdateRequest(
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
        @NotBlank @Size(max = 128) String reauthTicket,
        @NotBlank @Size(max = 500) String reason
) {
}
