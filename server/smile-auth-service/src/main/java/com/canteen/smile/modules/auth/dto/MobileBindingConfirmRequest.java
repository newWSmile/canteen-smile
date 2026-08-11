package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 首次绑定手机号确认请求。
 *
 * @param mobile 与挑战手机号摘要严格匹配的完整手机号
 * @param challengeId 短信挑战 ID
 * @param code 六位短信验证码
 */
public record MobileBindingConfirmRequest(
        @NotBlank @Size(max = 32) String mobile,
        @NotBlank @Size(max = 128) String challengeId,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code
) {
}
