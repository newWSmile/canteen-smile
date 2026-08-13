package com.canteen.smile.modules.navigation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改租户功能启停状态的敏感命令。
 *
 * @param enabled 是否启用功能
 * @param version 当前乐观锁版本
 * @param reauthTicket 当前所有者再认证票据
 * @param reason 修改原因
 */
public record UpdateTenantFeatureRequest(
        @NotNull Boolean enabled,
        @NotNull @Min(0) Long version,
        @NotBlank @Size(max = 256) String reauthTicket,
        @NotBlank @Size(max = 500) String reason
) { }
