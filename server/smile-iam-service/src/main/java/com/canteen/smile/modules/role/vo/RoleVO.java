package com.canteen.smile.modules.role.vo;

import java.time.OffsetDateTime;

/**
 * 本机构角色响应。
 *
 * @param id 角色 ID
 * @param roleCode 永久系统编码
 * @param name 名称
 * @param description 说明
 * @param roleType OWNER 或 CUSTOM
 * @param status 状态
 * @param authzVersion 授权版本
 * @param accountCount 当前关联账号数量
 * @param defaultScopeType 默认数据范围
 * @param createdTime 创建时间
 * @param version 乐观锁版本
 */
public record RoleVO(
        String id,
        String roleCode,
        String name,
        String description,
        String roleType,
        String status,
        long authzVersion,
        long accountCount,
        String defaultScopeType,
        OffsetDateTime createdTime,
        long version
) {
}
