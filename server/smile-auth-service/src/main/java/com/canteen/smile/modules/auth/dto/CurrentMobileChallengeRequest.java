package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 向当前账号已验证手机号发送敏感操作验证码的请求。 */
@Getter
@Setter
@NoArgsConstructor
public class CurrentMobileChallengeRequest {

    /** 当前浏览器稳定设备标识，用于设备维度限流。 */
    @NotBlank
    @Size(max = 128)
    private String deviceId;

    /** 达到风控门槛后使用的图形验证码票据。 */
    @Size(max = 256)
    private String captchaTicket;
}
