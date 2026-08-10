package com.canteen.smile.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * IAM 原子消费再认证票据的内部请求。
 *
 * @param ticket 五分钟高熵一次性票据
 * @param subjectType 再认证主体类型
 * @param subjectId 再认证主体 ID
 * @param allowedAction 票据绑定的唯一敏感动作
 */
public record ConsumeReauthTicketRequest(
        @NotBlank @Size(max = 128) String ticket,
        @Pattern(regexp = "PLATFORM_IDENTITY|TENANT_ACCOUNT") String subjectType,
        @Pattern(regexp = "^[1-9][0-9]*$") String subjectId,
        @NotBlank @Size(max = 128) String allowedAction
) {
}
