package com.canteen.smile.modules.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 手机号验证码登录请求；手机号由已验证短信挑战确定，不再次接收明文。 */
@Getter
@Setter
@NoArgsConstructor
public class SmsLoginRequest {

    /** 租户应用入口编码。 */
    @NotBlank
    @Pattern(regexp = "TENANT_ADMIN|TENANT_PORTAL")
    private String appCode;

    /** 已发送 LOGIN 用途验证码的挑战标识。 */
    @NotBlank
    @Size(max = 64)
    private String challengeId;

    /** 用户输入的六位短信验证码。 */
    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;

    /** 是否申请记住我会话。 */
    private boolean rememberMe;

    /** 当前登录设备描述。 */
    @Valid
    @NotNull
    private DeviceRequest device;
}
