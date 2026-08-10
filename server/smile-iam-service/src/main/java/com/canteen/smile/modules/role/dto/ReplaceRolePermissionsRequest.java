package com.canteen.smile.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 整版替换角色功能权限请求。
 *
 * @param permissionIds 完整权限资源 ID 集合
 * @param version 角色乐观锁版本
 * @param reason 授权原因
 */
public record ReplaceRolePermissionsRequest(
        @Size(max = 1000) List<@Pattern(regexp = "[1-9][0-9]*") String> permissionIds,
        @PositiveOrZero long version,
        @NotBlank @Size(max = 500) String reason
) {
    /** 保证集合不为 null 且不可被外部修改。 */
    public ReplaceRolePermissionsRequest {
        permissionIds = permissionIds == null ? List.of() : List.copyOf(permissionIds);
    }
}
