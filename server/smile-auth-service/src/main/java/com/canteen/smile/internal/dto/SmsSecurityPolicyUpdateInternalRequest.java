package com.canteen.smile.internal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** IAM 修改验证码明文留存开关的内部签名请求。 */
public record SmsSecurityPolicyUpdateInternalRequest(
        boolean plaintextCodeRetentionEnabled,
        @Min(0) long version,
        @Positive long actorId,
        @NotBlank @Size(max = 128) String reauthTicket
) {
}
