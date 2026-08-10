package com.canteen.smile.modules.account.vo;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 本机构用户响应。
 *
 * @param id 账号 ID
 * @param username 用户名
 * @param displayName 显示名称
 * @param employeeNumber 工号
 * @param organizationId 所属机构 ID
 * @param organizationName 所属机构名称
 * @param status 账号状态
 * @param validityMode 有效期模式
 * @param effectiveAt 生效时间
 * @param expiresAt 到期时间
 * @param roles 当前有效角色
 * @param owner 是否机构所有者
 * @param authzVersion 授权版本
 * @param createdTime 创建时间
 * @param version 乐观锁版本
 */
public record TenantUserVO(
        String id, String username, String displayName, String employeeNumber,
        String organizationId, String organizationName, String status, String validityMode,
        OffsetDateTime effectiveAt, OffsetDateTime expiresAt, List<TenantUserRoleVO> roles,
        boolean owner, long authzVersion, OffsetDateTime createdTime, long version
) {
}
