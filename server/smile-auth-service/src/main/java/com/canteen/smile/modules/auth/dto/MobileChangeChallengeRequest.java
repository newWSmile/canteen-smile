package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建新手机号换绑验证码挑战的安全请求。
 *
 * @param reauthTicket 仅允许手机号换绑的五分钟一次性再认证票据
 * @param mobile 待验证的新手机号
 * @param deviceId 当前设备稳定标识
 * @param captchaTicket 风控要求的人机校验票据
 */
public record MobileChangeChallengeRequest(
        @NotBlank @Size(max = 256) String reauthTicket,
        @NotBlank @Size(max = 32) String mobile,
        @NotBlank @Size(max = 128) String deviceId,
        @Size(max = 256) String captchaTicket
) {
}
