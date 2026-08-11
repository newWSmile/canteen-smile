package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 完成手机号换绑请求。
 *
 * @param reauthTicket 当前手机号或当前密码验证签发的五分钟再认证票据
 * @param newMobile 已完成新手机号挑战的完整号码
 * @param newChallengeId 新手机号短信挑战标识
 * @param newCode 新手机号收到的六位验证码
 */
public record MobileChangeConfirmRequest(
        @NotBlank @Size(max = 256) String reauthTicket,
        @NotBlank @Size(max = 32) String newMobile,
        @NotBlank @Size(max = 128) String newChallengeId,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String newCode
) {
}
