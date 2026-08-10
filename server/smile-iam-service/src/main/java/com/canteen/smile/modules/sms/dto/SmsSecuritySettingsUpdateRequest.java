package com.canteen.smile.modules.sms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 平台修改验证码明文留存安全开关的请求。 */
public record SmsSecuritySettingsUpdateRequest(
        boolean plaintextCodeRetentionEnabled,
        @Min(0) long version,
        @NotBlank @Size(max = 128) String reauthTicket,
        @NotBlank @Size(max = 500) String reason
) {
}
