package com.canteen.smile.modules.navigation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改租户统一菜单显示状态的敏感命令。
 *
 * @param hidden 是否对租户全部机构隐藏
 * @param version 当前乐观锁版本
 * @param reauthTicket 当前管理员再认证票据
 * @param reason 修改原因
 */
public record UpdateTenantMenuVisibilityRequest(
        @NotNull Boolean hidden,
        @NotNull @Min(0) Long version,
        @NotBlank @Size(max = 256) String reauthTicket,
        @NotBlank @Size(max = 500) String reason
) { }
