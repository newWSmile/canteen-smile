package com.canteen.smile.modules.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 手机号验证成功后选择具体租户账号的登录请求。 */
@Getter
@Setter
@NoArgsConstructor
public class AccountSelectionLoginRequest {

    /** 租户应用入口编码。 */
    @NotBlank
    @Pattern(regexp = "TENANT_ADMIN|TENANT_PORTAL")
    private String appCode;

    /** Auth 签发的短期一次性账号选择票据。 */
    @NotBlank
    @Size(max = 256)
    private String accountSelectorTicket;

    /** 用户从已验证候选集合中选择的租户账号 ID。 */
    @NotBlank
    @Pattern(regexp = "[1-9]\\d{0,18}")
    private String accountId;

    /** 是否申请记住我会话。 */
    private boolean rememberMe;

    /** 当前登录设备描述。 */
    @Valid
    @NotNull
    private DeviceRequest device;
}
