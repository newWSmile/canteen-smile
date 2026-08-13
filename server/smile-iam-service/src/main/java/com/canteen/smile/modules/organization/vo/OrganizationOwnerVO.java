package com.canteen.smile.modules.organization.vo;

import java.time.OffsetDateTime;

/**
 * 机构所有者摘要。
 *
 * @param organizationId 机构 ID
 * @param accountId 所有者账号 ID
 * @param username 所有者用户名
 * @param displayName 所有者显示名称
 * @param effectiveTime 生效时间
 * @param version 所有者关系版本
 */
public record OrganizationOwnerVO(
        String organizationId,
        String accountId,
        String username,
        String displayName,
        OffsetDateTime effectiveTime,
        long version
) {
}
