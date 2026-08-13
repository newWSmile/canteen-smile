package com.canteen.smile.modules.organization.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

/** 机构所有权关系数据访问接口。 */
public interface OrganizationOwnerMapper {
    /** @return 当前有效所有者 */
    OwnerRow selectOwner(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId);
    /** 更新所有者关系。 */
    int transferOwner(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                      @Param("fromAccountId") long fromAccountId, @Param("toAccountId") long toAccountId,
                      @Param("version") long version, @Param("operatorId") long operatorId);
    /** 删除原所有者保护角色绑定。 */
    int deactivateOwnerRole(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                            @Param("accountId") long accountId, @Param("roleId") long roleId,
                            @Param("operatorId") long operatorId);
    /** 为新所有者绑定保护角色。 */
    int insertOwnerRole(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                        @Param("accountId") long accountId, @Param("roleId") long roleId,
                        @Param("operatorId") long operatorId);
    /** 写入只追加转让历史。 */
    int insertHistory(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                      @Param("fromAccountId") long fromAccountId, @Param("toAccountId") long toAccountId,
                      @Param("reason") String reason, @Param("operatorId") long operatorId);
    /** 提升账号授权版本。 */
    int bumpAccountVersion(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                           @Param("accountId") long accountId, @Param("operatorId") long operatorId);
    /** 当前所有者关系投影。 */
    record OwnerRow(long organizationId, long accountId, long protectedRoleId, String username,
                    String displayName, OffsetDateTime effectiveTime, long version) { }
}
