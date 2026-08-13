package com.canteen.smile.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 机构所有权转让请求。
 *
 * @param targetAccountId 新所有者账号 ID
 * @param reauthTicket 当前所有者密码再认证票据
 * @param reason 转让原因
 * @param version 所有者关系乐观锁版本
 */
public record TransferOrganizationOwnerRequest(
        @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String targetAccountId,
        @NotBlank @Size(max = 128) String reauthTicket,
        @NotBlank @Size(max = 500) String reason,
        @PositiveOrZero long version
) {
}
