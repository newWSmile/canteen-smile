package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 使用 PASSWORD_RESET 用途短信验证码开始自助找回密码的请求。 */
@Getter
@Setter
@NoArgsConstructor
public class SmsPasswordResetVerificationRequest {

    /** 租户应用入口编码。 */
    @NotBlank
    @Pattern(regexp = "TENANT_ADMIN|TENANT_PORTAL")
    private String appCode;

    /** Auth 已签发的 PASSWORD_RESET 用途短信挑战标识。 */
    @NotBlank
    @Size(max = 64)
    private String challengeId;

    /** 用户收到的六位短信验证码。 */
    @NotBlank
    @Pattern(regexp = "[0-9]{6}")
    private String code;
}
