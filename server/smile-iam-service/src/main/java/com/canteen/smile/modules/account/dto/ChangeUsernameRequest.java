package com.canteen.smile.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 当前账号修改用户名请求。
 *
 * @param username 新用户名
 * @param reauthTicket 当前密码再认证票据
 * @param reason 修改原因
 */
public record ChangeUsernameRequest(
        @NotBlank @Size(min = 3, max = 128)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{2,127}$") String username,
        @NotBlank @Size(max = 128) String reauthTicket,
        @NotBlank @Size(max = 500) String reason
) {
}
