package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 平台管理员以一次性恢复码完成二次验证的请求。 */
@Getter
@Setter
@NoArgsConstructor
public class PlatformRecoveryLoginRequest {

    /** 密码校验通过后签发的短期一次性票据。 */
    @NotBlank
    @Size(max = 256)
    private String secondFactorTicket;

    /** 首次引导时只展示一次的平台恢复码。 */
    @NotBlank
    @Size(max = 128)
    private String recoveryCode;
}
