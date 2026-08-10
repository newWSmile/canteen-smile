package com.canteen.smile.modules.permission.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建平台权限资源请求。
 *
 * @param permissionCode 永久唯一权限码
 * @param resourceType 资源类型
 * @param parentId 可选父资源 ID
 * @param name 资源名称
 * @param description 可选说明
 * @param appCode 应用编码
 * @param routePath 可选前端路由
 * @param componentKey 可选本地组件键
 * @param apiMethod API 方法
 * @param apiPathPattern API 模板路径
 * @param featureCode 可选功能开关编码
 * @param semanticVersion 语义版本
 * @param sortOrder 同级排序
 */
public record CreatePermissionResourceRequest(
        @NotBlank @Size(max = 128)
        @Pattern(regexp = "[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*){2,3}") String permissionCode,
        @NotBlank @Pattern(regexp = "DIRECTORY|MENU|BUTTON|API") String resourceType,
        @Pattern(regexp = "[1-9][0-9]*") String parentId,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 500) String description,
        @NotBlank @Pattern(regexp = "PLATFORM_ADMIN|TENANT_ADMIN|TENANT_PORTAL|SERVICE") String appCode,
        @Size(max = 256) String routePath,
        @Size(max = 128) String componentKey,
        @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE") String apiMethod,
        @Size(max = 256) String apiPathPattern,
        @Size(max = 128) String featureCode,
        @Min(1) @Max(100000) int semanticVersion,
        @Min(0) @Max(100000) int sortOrder
) {
}
