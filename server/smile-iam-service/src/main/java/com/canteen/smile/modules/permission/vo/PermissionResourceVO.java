package com.canteen.smile.modules.permission.vo;

import java.time.OffsetDateTime;

/**
 * 平台权限资源响应。
 *
 * @param id 资源 ID
 * @param permissionCode 永久权限码
 * @param resourceType 资源类型
 * @param parentId 父资源 ID
 * @param name 名称
 * @param description 说明
 * @param appCode 应用编码
 * @param routePath 前端路由
 * @param componentKey 本地组件键
 * @param apiMethod HTTP 方法
 * @param apiPathPattern API 模板路径
 * @param featureCode 功能开关编码
 * @param publishStatus 发布状态
 * @param semanticVersion 语义版本
 * @param sortOrder 排序值
 * @param createdTime 创建时间
 * @param version 乐观锁版本
 */
public record PermissionResourceVO(
        String id,
        String permissionCode,
        String resourceType,
        String parentId,
        String name,
        String description,
        String appCode,
        String routePath,
        String componentKey,
        String apiMethod,
        String apiPathPattern,
        String featureCode,
        String publishStatus,
        int semanticVersion,
        int sortOrder,
        OffsetDateTime createdTime,
        long version
) {
}
