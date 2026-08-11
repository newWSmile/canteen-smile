package com.canteen.smile.modules.auth.dto;

import com.canteen.smile.modules.auth.model.ReauthAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 当前手机号验证码再认证请求。
 *
 * @param challengeId 当前手机号短信挑战标识
 * @param code 六位短信验证码
 * @param allowedAction 票据唯一允许的换绑或解绑动作
 */
public record CurrentMobileVerificationRequest(
        @NotBlank @Size(max = 128) String challengeId,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code,
        @NotNull ReauthAction allowedAction
) {
}
