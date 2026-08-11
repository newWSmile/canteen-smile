package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 当前租户账号首次绑定手机号的验证码挑战请求。 */
@Getter
@Setter
@NoArgsConstructor
public class MobileBindingChallengeRequest {

    /** 待验证完整手机号，只在 Auth 当前调用链和短信 Client 内使用。 */
    @NotBlank
    @Size(max = 32)
    private String mobile;

    /** 当前浏览器稳定设备标识，用于设备维度限流。 */
    @NotBlank
    @Size(max = 128)
    private String deviceId;

    /** 达到风控门槛后使用的图形验证码票据。 */
    @Size(max = 256)
    private String captchaTicket;
}
