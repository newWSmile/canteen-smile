package com.canteen.smile.modules.account.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 租户账号激活和登录上下文数据访问接口。 */
public interface AccountLifecycleMapper {

    /** @param accountId 租户账号 ID @return 激活上下文，不存在时为空 */
    ActivationContextRow selectActivationContext(@Param("accountId") long accountId);

    /** @param tenantId 租户 ID @return 根机构所有者激活上下文，不存在时为空 */
    ActivationContextRow selectRootOwnerActivationContext(@Param("tenantId") long tenantId);

    /**
     * 批量查询租户根机构所有者账号状态，避免租户列表产生 N+1 查询。
     *
     * @param tenantIds 当前分页中的租户 ID
     * @return 租户与根机构所有者账号状态
     */
    List<TenantOwnerSummaryRow> selectRootOwnerSummaries(@Param("tenantIds") List<Long> tenantIds);

    /** @param accountId 租户账号 ID @return 实际激活行数 */
    int activatePendingAccount(@Param("accountId") long accountId);

    /**
     * 使租户账号进入必须重置密码状态。
     *
     * @param accountId 租户账号 ID
     * @param operatorId 发起平台身份 ID
     * @return 更新行数
     */
    int markPasswordResetRequired(
            @Param("accountId") long accountId,
            @Param("operatorId") long operatorId
    );

    /** @param accountId 租户账号 ID @return 完成密码恢复的更新行数 */
    int completePasswordReset(@Param("accountId") long accountId);

    /** @param normalizedUsername 归一化用户名 @return 可登录租户账号上下文 */
    LoginContextRow selectLoginContext(@Param("normalizedUsername") String normalizedUsername);

    /**
     * 批量查询当前仍可登录的租户账号候选，数据状态与有效期校验全部下推数据库。
     *
     * @param accountIds Auth 已验证手机号对应的账号 ID，最多一百个
     * @return 可登录账号候选
     */
    List<MobileLoginCandidateRow> selectMobileLoginCandidates(@Param("accountIds") List<Long> accountIds);

    /** @param accountId 租户账号 ID @return 权限计算与启动上下文，不存在时为空 */
    TenantPermissionContextRow selectTenantPermissionContext(@Param("accountId") long accountId);

    /**
     * 账号激活所需且可向 Auth 暴露的最小上下文。
     *
     * @param accountId 账号 ID
     * @param tenantId 租户 ID
     * @param organizationId 机构 ID
     * @param username 用户名
     * @param displayName 显示名称
     * @param tenantName 租户名称
     * @param organizationName 机构名称
     * @param accountStatus 账号状态
     * @param tenantStatus 租户状态
     */
    record ActivationContextRow(
            long accountId,
            long tenantId,
            long organizationId,
            String username,
            String displayName,
            String tenantName,
            String organizationName,
            String accountStatus,
            String tenantStatus
    ) {
    }

    /**
     * Auth 建立租户设备会话所需的最小已验证快照。
     *
     * @param accountId 账号 ID
     * @param tenantId 租户 ID
     * @param organizationId 机构 ID
     * @param username 用户名
     * @param displayName 显示名称
     * @param accountStatus 账号状态
     * @param tenantStatus 租户状态
     * @param organizationStatus 机构状态
     * @param authzVersion 授权版本
     * @param concurrentLoginEnabled 是否允许多设备并发
     * @param maxDevices 最大设备数
     * @param rememberMeEnabled 是否允许记住我
     * @param idleSeconds 普通空闲秒数
     * @param absoluteSeconds 普通最长秒数
     * @param rememberIdleSeconds 记住我空闲秒数
     * @param rememberAbsoluteSeconds 记住我最长秒数
     */
    record LoginContextRow(
            long accountId,
            long tenantId,
            long organizationId,
            String username,
            String displayName,
            String accountStatus,
            String tenantStatus,
            String organizationStatus,
            long authzVersion,
            boolean concurrentLoginEnabled,
            int maxDevices,
            boolean rememberMeEnabled,
            int idleSeconds,
            int absoluteSeconds,
            int rememberIdleSeconds,
            int rememberAbsoluteSeconds
    ) {
    }

    /**
     * 手机号登录批量解析使用的租户账号与安全策略快照。
     *
     * @param accountId 账号 ID
     * @param tenantId 租户 ID
     * @param tenantName 租户名称
     * @param organizationId 所属机构 ID
     * @param organizationName 所属机构名称
     * @param username 用户名
     * @param displayName 显示名称
     * @param authzVersion 授权版本
     * @param concurrentLoginEnabled 是否允许多设备并发
     * @param maxDevices 最大设备数
     * @param rememberMeEnabled 是否允许记住我
     * @param idleSeconds 普通会话空闲秒数
     * @param absoluteSeconds 普通会话最长秒数
     * @param rememberIdleSeconds 记住我会话空闲秒数
     * @param rememberAbsoluteSeconds 记住我会话最长秒数
     */
    record MobileLoginCandidateRow(
            long accountId,
            long tenantId,
            String tenantName,
            long organizationId,
            String organizationName,
            String username,
            String displayName,
            long authzVersion,
            boolean concurrentLoginEnabled,
            int maxDevices,
            boolean rememberMeEnabled,
            int idleSeconds,
            int absoluteSeconds,
            int rememberIdleSeconds,
            int rememberAbsoluteSeconds
    ) {
    }

    /**
     * 平台租户列表使用的根机构所有者摘要行。
     *
     * @param tenantId 租户 ID
     * @param username 根机构所有者用户名
     * @param accountStatus 根机构所有者账号状态
     */
    record TenantOwnerSummaryRow(long tenantId, String username, String accountStatus) {
    }

    /**
     * 租户账号权限计算和管理端启动所需的真实上下文。
     *
     * @param accountId 账号 ID
     * @param tenantId 租户 ID
     * @param tenantName 租户名称
     * @param organizationId 所属机构 ID
     * @param organizationName 所属机构名称
     * @param rootOrganizationId 租户根机构 ID
     * @param username 用户名
     * @param displayName 显示名称
     * @param accountStatus 账号状态
     * @param tenantStatus 租户状态
     * @param organizationStatus 机构自身状态
     * @param rootOwner 是否租户根机构所有者
     */
    record TenantPermissionContextRow(
            long accountId,
            long tenantId,
            String tenantName,
            long organizationId,
            String organizationName,
            long rootOrganizationId,
            String username,
            String displayName,
            String accountStatus,
            String tenantStatus,
            String organizationStatus,
            boolean rootOwner
    ) {
    }
}
