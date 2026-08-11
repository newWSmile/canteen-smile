package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 完成手机号解绑请求。
 *
 * @param reauthTicket 当前手机号或当前密码验证签发的五分钟再认证票据
 */
public record MobileUnbindConfirmRequest(
        @NotBlank @Size(max = 256) String reauthTicket
) {
}
