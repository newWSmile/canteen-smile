package com.canteen.smile.modules.role.mapper;

import com.canteen.smile.modules.role.entity.RoleEntity;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** 机构角色、权限和数据范围数据访问接口。 */
public interface RoleMapper {

    /** @return 下一个角色主键 */
    long nextRoleId();
    /** @return 下一个数据策略主键 */
    long nextDataPolicyId();
    /** @return 下一个 Outbox 主键 */
    /** @return 当前机构角色数量 */
    long countRoles(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                    @Param("status") String status);
    /** @return 当前机构角色分页 */
    List<RoleRow> selectRoles(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                              @Param("status") String status, @Param("offset") int offset,
                              @Param("limit") int limit);
    /** @return 指定当前机构角色 */
    RoleRow selectRole(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                       @Param("roleId") long roleId);
    /** @return 新增角色行数 */
    int insertRole(RoleEntity entity);
    /** @return 新增数据策略行数 */
    int insertDataPolicy(@Param("policyId") long policyId, @Param("tenantId") long tenantId,
                         @Param("organizationId") long organizationId, @Param("roleId") long roleId,
                         @Param("moduleCode") String moduleCode, @Param("scopeType") String scopeType,
                         @Param("operatorId") long operatorId);
    /** 批量新增策略指定机构。 */
    int insertDataPolicyOrganizations(@Param("tenantId") long tenantId, @Param("policyId") long policyId,
                                      @Param("organizationIds") List<Long> organizationIds,
                                      @Param("operatorId") long operatorId);
    /** @return 修改角色行数 */
    int updateRole(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                   @Param("roleId") long roleId, @Param("name") String name,
                   @Param("normalizedName") String normalizedName, @Param("description") String description,
                   @Param("version") long version, @Param("operatorId") long operatorId);
    /** @return 修改状态行数 */
    int updateRoleStatus(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                         @Param("roleId") long roleId, @Param("status") String status,
                         @Param("version") long version, @Param("operatorId") long operatorId);
    /** @return 逻辑删除角色行数 */
    int deleteRole(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                   @Param("roleId") long roleId, @Param("version") long version,
                   @Param("operatorId") long operatorId);
    /** @return 当前角色关联账号数量 */
    long countRoleAccounts(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                           @Param("roleId") long roleId);
    /** @return 当前操作者可授予权限资源 */
    List<PermissionRow> selectGrantablePermissions(@Param("tenantId") long tenantId,
                                                    @Param("accountId") long accountId,
                                                    @Param("rootOwner") boolean rootOwner);
    /** @return 当前角色已有权限 ID */
    List<Long> selectRolePermissionIds(@Param("tenantId") long tenantId,
                                       @Param("organizationId") long organizationId,
                                       @Param("roleId") long roleId);
    /** @return 请求权限中属于操作者授权上限的数量 */
    long countGrantablePermissionIds(@Param("tenantId") long tenantId, @Param("accountId") long accountId,
                                     @Param("rootOwner") boolean rootOwner,
                                     @Param("permissionIds") List<Long> permissionIds);
    /** 逻辑删除角色当前权限。 */
    int deactivateRolePermissions(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                                  @Param("roleId") long roleId, @Param("operatorId") long operatorId);
    /** 整版写入角色权限。 */
    int upsertRolePermissions(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                              @Param("roleId") long roleId, @Param("permissionIds") List<Long> permissionIds,
                              @Param("operatorId") long operatorId);
    /** 仅提升角色授权版本和乐观锁版本。 */
    int touchRoleAuthorization(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                               @Param("roleId") long roleId, @Param("version") long version,
                               @Param("operatorId") long operatorId);
    /** 逻辑删除角色数据策略关联机构。 */
    int deactivateRoleDataScopeOrganizations(@Param("tenantId") long tenantId,
                                             @Param("organizationId") long organizationId,
                                             @Param("roleId") long roleId,
                                             @Param("operatorId") long operatorId);
    /** 逻辑删除角色数据策略。 */
    int deactivateRoleDataPolicies(@Param("tenantId") long tenantId, @Param("organizationId") long organizationId,
                                   @Param("roleId") long roleId, @Param("operatorId") long operatorId);
    /** @return 角色数据策略明细 */
    List<DataPolicyRow> selectRoleDataPolicies(@Param("tenantId") long tenantId,
                                               @Param("organizationId") long organizationId,
                                               @Param("roleId") long roleId);
    /** @return 请求模块中已发布模块数量 */
    long countPublishedDataModules(@Param("moduleCodes") List<String> moduleCodes);
    /** @return 指定机构 ID 中位于操作者机构子树的数量 */
    long countOrganizationsInActorSubtree(@Param("tenantId") long tenantId,
                                          @Param("actorOrganizationId") long actorOrganizationId,
                                          @Param("organizationIds") List<Long> organizationIds);
    /** 批量提升受角色影响账号授权版本。 */
    int bumpAssignedAccountAuthzVersions(@Param("tenantId") long tenantId,
                                         @Param("organizationId") long organizationId,
                                         @Param("roleId") long roleId,
                                         @Param("operatorId") long operatorId);
    /** 新增等待阶段 4 投递的角色授权变更事件。 */
    int insertRoleAuthorizationOutbox(@Param("eventIdSeed") String eventIdSeed,
                                      @Param("tenantId") long tenantId,
                                      @Param("organizationId") long organizationId,
                                      @Param("roleId") long roleId,
                                      @Param("operatorId") long operatorId);
    /** @return 普通租户账号当前有效权限码 */
    List<String> selectEffectivePermissionCodes(@Param("tenantId") long tenantId,
                                                @Param("accountId") long accountId);
    /** @return 普通租户账号当前有效角色编码 */
    List<String> selectEffectiveRoleCodes(@Param("tenantId") long tenantId,
                                          @Param("organizationId") long organizationId,
                                          @Param("accountId") long accountId);

    /** 角色分页投影。 */
    record RoleRow(long id, String roleCode, String name, String description, String roleType, String status,
                   long authzVersion, long accountCount, String defaultScopeType,
                   OffsetDateTime createdTime, long version) {
    }

    /** 可授予权限资源投影。 */
    record PermissionRow(long id, Long parentId, String permissionCode, String name, String resourceType,
                         String appCode, String featureCode, int sortOrder) {
    }

    /** 数据策略与指定机构平铺投影。 */
    record DataPolicyRow(long policyId, String moduleCode, String moduleName, String scopeType,
                         Long organizationId) {
    }
}
