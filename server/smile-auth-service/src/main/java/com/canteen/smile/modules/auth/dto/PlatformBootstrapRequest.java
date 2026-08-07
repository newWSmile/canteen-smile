package com.canteen.smile.modules.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 首位平台超级管理员一次性引导请求。 */
@Getter
@Setter
public class PlatformBootstrapRequest {

    /** 全平台唯一用户名。 */
    @NotBlank(message = "username 不能为空")
    @Size(max = 128, message = "username 长度不能超过 128")
    private String username;

    /** 可选显示名称。 */
    @Size(max = 128, message = "displayName 长度不能超过 128")
    private String displayName;

    /** 仅允许 Auth 原子消费和解密的一次性密码信封。 */
    @Valid
    @NotNull(message = "passwordEnvelope 不能为空")
    private PasswordEnvelopeRequest passwordEnvelope;
}
