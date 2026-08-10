package com.canteen.smile.modules.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 用户状态命令请求。
 *
 * @param reason 必填操作原因
 * @param version 用户乐观锁版本
 */
public record TenantUserStatusRequest(
        @NotBlank @Size(max = 500) String reason,
        @PositiveOrZero long version
) {
}
