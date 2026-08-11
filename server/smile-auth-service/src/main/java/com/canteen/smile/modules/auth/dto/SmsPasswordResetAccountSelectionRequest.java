package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 手机号绑定多个账号时选择具体找回账号的请求。 */
@Getter
@Setter
@NoArgsConstructor
public class SmsPasswordResetAccountSelectionRequest {

    /** 租户应用入口编码。 */
    @NotBlank
    @Pattern(regexp = "TENANT_ADMIN|TENANT_PORTAL")
    private String appCode;

    /** Auth 签发且仅允许 PASSWORD_RESET 流程消费的账号选择票据。 */
    @NotBlank
    @Size(max = 256)
    private String accountSelectorTicket;

    /** 用户从已验证候选集合中选择的租户账号 ID。 */
    @NotBlank
    @Pattern(regexp = "[1-9]\\d{0,18}")
    private String accountId;
}
