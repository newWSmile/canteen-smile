package com.canteen.smile.modules.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Auth 按用户名解析登录主体的内部请求。 */
public record UsernameLoginResolutionRequest(
        @NotBlank(message = "appCode 不能为空")
        @Pattern(regexp = "PLATFORM_ADMIN|TENANT_ADMIN|TENANT_PORTAL", message = "appCode 无效")
        String appCode,

        @NotBlank(message = "username 不能为空")
        @Size(max = 128, message = "username 长度不能超过 128")
        String username
) {
}
