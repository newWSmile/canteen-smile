package com.canteen.smile.modules.account.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** 本机构用户、角色绑定和账号初始化 Outbox 数据访问接口。 */
public interface TenantUserMapper {
    /** @return 下一个账号 ID */
    long nextAccountId();
    /** @return 下一个 Outbox ID */
    long nextOutboxId();
    /** @return 匹配用户数量 */
    long countUsers(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                    @Param("keyword") String keyword, @Param("status") String status);
    /** @return 本机构用户分页 */
    List<UserRow> selectUsers(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                              @Param("keyword") String keyword, @Param("status") String status,
                              @Param("offset") int offset, @Param("limit") int limit);
    /** @return 指定本机构用户 */
    UserRow selectUser(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                       @Param("accountId") long accountId);
    /** @return 一批账号当前有效角色 */
    List<UserRoleRow> selectUserRoles(@Param("tenantId") long tenantId,
                                      @Param("organizationId") long organizationId,
                                      @Param("accountIds") List<Long> accountIds);
    /** @return 全局用户名是否已经永久占用 */
    long countReservedUsername(@Param("normalizedUsername") String normalizedUsername);
    /** @return 本机构工号是否已经永久占用 */
    long countReservedEmployeeNumber(@Param("tenantId") long tenantId,
                                     @Param("organizationId") long organizationId,
                                     @Param("normalizedEmployeeNumber") String normalizedEmployeeNumber);
    /** @return 输入角色中当前操作者可授予的角色数量 */
    long countAssignableRoles(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                              @Param("actorAccountId") long actorAccountId,
                              @Param("rootOwner") boolean rootOwner, @Param("roleIds") List<Long> roleIds);
    /** 插入待激活账号。 */
    int insertAccount(@Param("accountId") long accountId, @Param("tenantId") long tenantId,
                      @Param("organizationId") long organizationId, @Param("username") String username,
                      @Param("normalizedUsername") String normalizedUsername,
                      @Param("displayName") String displayName, @Param("employeeNumber") String employeeNumber,
                      @Param("validityMode") String validityMode, @Param("effectiveAt") OffsetDateTime effectiveAt,
                      @Param("expiresAt") OffsetDateTime expiresAt, @Param("operatorId") long operatorId);
    /** 永久登记用户名。 */
    int insertUsernameRegistry(@Param("accountId") long accountId, @Param("username") String username,
                               @Param("normalizedUsername") String normalizedUsername,
                               @Param("operatorId") long operatorId);
    /** 永久登记可选工号。 */
    int insertEmployeeNumberRegistry(@Param("accountId") long accountId, @Param("tenantId") long tenantId,
                                     @Param("organizationId") long organizationId,
                                     @Param("employeeNumber") String employeeNumber,
                                     @Param("normalizedEmployeeNumber") String normalizedEmployeeNumber,
                                     @Param("operatorId") long operatorId);
    /** 批量绑定账号初始角色。 */
    int insertAccountRoles(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                           @Param("accountId") long accountId, @Param("roleIds") List<Long> roleIds,
                           @Param("operatorId") long operatorId);
    /** 写入账号凭证初始化 Outbox。 */
    int insertProvisionOutbox(@Param("outboxId") long outboxId, @Param("eventId") String eventId,
                              @Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                              @Param("accountId") long accountId, @Param("operatorId") long operatorId);
    /** 标记账号凭证初始化已经投递。 */
    int markProvisionPublished(@Param("outboxId") long outboxId, @Param("operatorId") long operatorId);
    /** 标记账号凭证初始化等待重试。 */
    int markProvisionRetry(@Param("outboxId") long outboxId, @Param("operatorId") long operatorId,
                           @Param("errorCode") String errorCode);
    /** 乐观锁提升账号授权版本。 */
    int bumpAuthzVersion(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                         @Param("accountId") long accountId, @Param("version") long version,
                         @Param("operatorId") long operatorId);
    /** 逻辑移除用户当前角色。 */
    int deactivateAccountRoles(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                               @Param("accountId") long accountId, @Param("operatorId") long operatorId);
    /** 写入用户角色变化事件。 */
    int insertRolesChangedOutbox(@Param("outboxId") long outboxId, @Param("eventId") String eventId,
                                 @Param("tenantId") long tenantId, @Param("accountId") long accountId,
                                 @Param("operatorId") long operatorId,
                                 @Param("ipAddress") String ipAddress);
    /** 修改用户显示资料和有效期。 */
    int updateUserProfile(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                          @Param("accountId") long accountId, @Param("displayName") String displayName,
                          @Param("employeeNumber") String employeeNumber,
                          @Param("validityMode") String validityMode,
                          @Param("effectiveAt") OffsetDateTime effectiveAt,
                          @Param("expiresAt") OffsetDateTime expiresAt,
                          @Param("authzChanged") boolean authzChanged, @Param("version") long version,
                          @Param("operatorId") long operatorId);
    /** 乐观锁改变用户生命周期状态并提升授权版本。 */
    int changeUserStatus(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                         @Param("accountId") long accountId, @Param("currentStatus") String currentStatus,
                         @Param("targetStatus") String targetStatus, @Param("version") long version,
                         @Param("operatorId") long operatorId);
    /** 注销时永久关闭当前用户名的登录入口。 */
    int disableUsernameLogin(@Param("accountId") long accountId, @Param("operatorId") long operatorId);
    /** 写入等待 Auth 消费的账号授权或状态变化事件。 */
    int insertAccountChangedOutbox(@Param("outboxId") long outboxId, @Param("eventId") String eventId,
                                   @Param("tenantId") long tenantId, @Param("accountId") long accountId,
                                   @Param("eventType") String eventType,
                                   @Param("actionNameSnapshot") String actionNameSnapshot,
                                   @Param("operatorId") long operatorId,
                                   @Param("ipAddress") String ipAddress);

    /** 用户分页投影。 */
    record UserRow(long id, String username, String displayName, String employeeNumber,
                   long organizationId, String organizationName, String status, String validityMode,
                   OffsetDateTime effectiveAt, OffsetDateTime expiresAt, boolean owner,
                   long authzVersion, OffsetDateTime createdTime, long version) { }
    /** 用户角色投影。 */
    record UserRoleRow(long accountId, long roleId, String roleName) { }
}
