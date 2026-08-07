package com.canteen.smile.modules.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 用户名密码登录请求，密码字段禁止进入日志。 */
@Getter
@Setter
@NoArgsConstructor
public class PasswordLoginRequest {

    /** 发起登录的前端应用编码。 */
    @NotBlank
    @Size(max = 32)
    private String appCode;

    /** 用户输入的用户名。 */
    @NotBlank
    @Size(max = 64)
    private String username;

    /** 一次性公钥挑战生成的密码加密信封。 */
    @Valid
    @NotNull
    private PasswordEnvelopeRequest passwordEnvelope;

    /** 是否申请记住我会话。 */
    private boolean rememberMe;

    /** 登录设备描述。 */
    @Valid
    @NotNull
    private DeviceRequest device;

    /** 图形验证码票据；只有达到失败门槛后才需要。 */
    @Size(max = 256)
    private String captchaTicket;
}
