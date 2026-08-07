package com.canteen.smile.modules.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Auth 编排首位平台身份创建的内部请求。 */
public record BootstrapPlatformIdentityRequest(
        @NotBlank(message = "username 不能为空")
        @Size(max = 128, message = "username 长度不能超过 128")
        String username,

        @Size(max = 128, message = "displayName 长度不能超过 128")
        String displayName
) {
}
