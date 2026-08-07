package com.canteen.smile.modules.tenant.mapper;

import com.canteen.smile.modules.tenant.dto.TenantSecurityPolicyRequest;
import org.apache.ibatis.annotations.Param;

/** 租户初始化事务的数据访问接口。 */
public interface TenantProvisionMapper {

    /** @return 新租户 ID */
    long nextTenantId();

    /** @return 新机构 ID */
    long nextOrganizationId();

    /** @return 新账号 ID */
    long nextAccountId();

    /** @return 新角色 ID */
    long nextRoleId();

    /** @return 新 Outbox 主键 ID */
    long nextOutboxId();

    /** @return 指定已发布模板类型数量 */
    long countPublishedTemplateTypes(@Param("templateVersion") long templateVersion);

    /** @return 指定模板版本和类型编码是否已发布 */
    long countPublishedRootType(@Param("templateVersion") long templateVersion, @Param("typeCode") String typeCode);

    /** @return 指定行政区域是否存在且有效 */
    long countActiveAdminRegion(@Param("adminRegionId") long adminRegionId);

    /** 尝试占用幂等键。 */
    int insertIdempotency(
            @Param("operatorId") long operatorId,
            @Param("keyHash") String keyHash,
            @Param("requestHash") String requestHash
    );

    /** @return 已存在的幂等记录 */
    IdempotencyRow selectIdempotencyForUpdate(@Param("operatorId") long operatorId, @Param("keyHash") String keyHash);

    /** 将幂等记录标记完成并保存非敏感结果引用。 */
    int completeIdempotency(
            @Param("operatorId") long operatorId,
            @Param("keyHash") String keyHash,
            @Param("responseReference") String responseReference
    );

    /** 新增初始化中的租户边界。 */
    int insertTenant(
            @Param("tenantId") long tenantId,
            @Param("tenantCode") String tenantCode,
            @Param("name") String name,
            @Param("templateVersion") long templateVersion,
            @Param("operatorId") long operatorId
    );

    /** 永久保留租户编码。 */
    int insertTenantCodeRegistry(
            @Param("tenantId") long tenantId,
            @Param("tenantCode") String tenantCode,
            @Param("operatorId") long operatorId
    );

    /** 从平台模板复制租户独立机构类型。 */
    int copyOrganizationTypes(
            @Param("tenantId") long tenantId,
            @Param("templateVersion") long templateVersion,
            @Param("operatorId") long operatorId
    );

    /** 从平台模板复制租户独立机构类型关系。 */
    int copyOrganizationTypeRelations(
            @Param("tenantId") long tenantId,
            @Param("templateVersion") long templateVersion,
            @Param("operatorId") long operatorId
    );

    /** @return 租户内指定机构类型 ID */
    Long selectOrganizationTypeId(@Param("tenantId") long tenantId, @Param("typeCode") String typeCode);

    /** 新增根机构。 */
    int insertRootOrganization(
            @Param("organizationId") long organizationId,
            @Param("tenantId") long tenantId,
            @Param("organizationTypeId") long organizationTypeId,
            @Param("businessCode") String businessCode,
            @Param("name") String name,
            @Param("normalizedName") String normalizedName,
            @Param("adminRegionId") Long adminRegionId,
            @Param("operatorId") long operatorId
    );

    /** 写入根机构自身闭包关系。 */
    int insertRootOrganizationClosure(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("operatorId") long operatorId
    );

    /** 永久保留根机构业务编码。 */
    int insertOrganizationCodeRegistry(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("normalizedCode") String normalizedCode,
            @Param("operatorId") long operatorId
    );

    /** 回填租户根机构。 */
    int bindRootOrganization(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("operatorId") long operatorId
    );

    /** 新增租户安全策略。 */
    int insertSecurityPolicy(
            @Param("tenantId") long tenantId,
            @Param("policy") TenantSecurityPolicyRequest policy,
            @Param("operatorId") long operatorId
    );

    /** 新增待激活机构所有者账号。 */
    int insertOwnerAccount(
            @Param("accountId") long accountId,
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("username") String username,
            @Param("normalizedUsername") String normalizedUsername,
            @Param("displayName") String displayName,
            @Param("employeeNumber") String employeeNumber,
            @Param("operatorId") long operatorId
    );

    /** 永久保留所有者用户名。 */
    int insertUsernameRegistry(
            @Param("accountId") long accountId,
            @Param("username") String username,
            @Param("normalizedUsername") String normalizedUsername,
            @Param("operatorId") long operatorId
    );

    /** 永久保留可选工号。 */
    int insertEmployeeNumberRegistry(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("accountId") long accountId,
            @Param("employeeNumber") String employeeNumber,
            @Param("normalizedEmployeeNumber") String normalizedEmployeeNumber,
            @Param("operatorId") long operatorId
    );

    /** 新增机构所有者保护角色。 */
    int insertOwnerRole(
            @Param("roleId") long roleId,
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("roleCode") String roleCode,
            @Param("operatorId") long operatorId
    );

    /** 初始化所有者默认数据范围。 */
    int insertOwnerDataPolicy(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("roleId") long roleId,
            @Param("accountId") long accountId
    );

    /** 绑定账号与所有者角色。 */
    int insertAccountRole(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("accountId") long accountId,
            @Param("roleId") long roleId
    );

    /** 建立机构当前唯一所有者。 */
    int insertOrganizationOwner(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("accountId") long accountId,
            @Param("roleId") long roleId
    );

    /** 初始化租户功能开关。 */
    int initializeTenantFeatures(@Param("tenantId") long tenantId, @Param("operatorId") long operatorId);

    /** 初始化租户菜单显示配置。 */
    int initializeTenantMenus(@Param("tenantId") long tenantId, @Param("operatorId") long operatorId);

    /** 追加租户创建审计记录。 */
    int insertCreateAudit(
            @Param("tenantId") long tenantId,
            @Param("operatorId") long operatorId,
            @Param("tenantCode") String tenantCode
    );

    /** 写入待投递的 Auth 凭证初始化事件。 */
    int insertProvisionOutbox(
            @Param("outboxId") long outboxId,
            @Param("eventId") String eventId,
            @Param("tenantId") long tenantId,
            @Param("accountId") long accountId,
            @Param("organizationId") long organizationId,
            @Param("operatorId") long operatorId
    );

    /** @return 指定租户的根机构所有者账号 ID */
    Long selectOwnerAccountId(@Param("tenantId") long tenantId);

    /** @return 指定租户的根机构 ID */
    Long selectRootOrganizationId(@Param("tenantId") long tenantId);

    /** @return 指定租户凭证初始化事件的 Outbox 主键 */
    Long selectProvisionOutboxId(@Param("tenantId") long tenantId);

    /** Auth 同步成功后激活租户编排状态并完成事件。 */
    int markProvisionSucceeded(@Param("tenantId") long tenantId, @Param("outboxId") long outboxId, @Param("operatorId") long operatorId);

    /** Auth 同步失败后记录可重试状态。 */
    int markProvisionFailed(
            @Param("tenantId") long tenantId,
            @Param("outboxId") long outboxId,
            @Param("operatorId") long operatorId,
            @Param("errorCode") String errorCode
    );

    /** 幂等命令记录。 */
    record IdempotencyRow(String requestHash, String responseReference, String status) {
    }

}
