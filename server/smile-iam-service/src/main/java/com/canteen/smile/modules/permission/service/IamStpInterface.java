package com.canteen.smile.modules.permission.service;

import cn.dev33.satoken.stp.StpInterface;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import com.canteen.smile.modules.role.mapper.RoleMapper;
import com.canteen.smile.modules.platform.entity.PlatformIdentityEntity;
import com.canteen.smile.modules.platform.mapper.PlatformIdentityMapper;
import com.canteen.smile.modules.platform.model.PlatformIdentityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashSet;

/** IAM 服务基于真实身份状态提供 Sa-Token 最终权限与角色集合。 */
@Component
@RequiredArgsConstructor
public class IamStpInterface implements StpInterface {

    /** 平台登录 ID 前缀。 */
    private static final String PLATFORM_LOGIN_PREFIX = "PLATFORM:";

    /** 平台超级管理员角色编码。 */
    private static final String PLATFORM_SUPER_ADMIN_ROLE = "PLATFORM_SUPER_ADMIN";

    /** 租户账号登录 ID 前缀。 */
    private static final String TENANT_LOGIN_PREFIX = "TENANT:";

    /** 机构所有者受保护角色标识。 */
    private static final String ORGANIZATION_OWNER_ROLE = "ORGANIZATION_OWNER";

    /** 平台身份数据访问接口。 */
    private final PlatformIdentityMapper platformIdentityMapper;

    /** 租户账号权限上下文数据访问接口。 */
    private final AccountLifecycleMapper accountLifecycleMapper;

    /** 角色授权数据访问接口。 */
    private final RoleMapper roleMapper;

    /**
     * 返回当前真实有效身份拥有的权限码。
     *
     * @param loginId Sa-Token 登录 ID
     * @param loginType Sa-Token 登录类型
     * @return 权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (activePlatformIdentity(loginId) != null) {
            return List.of(
                        IamPermissionCodes.PLATFORM_TENANT_VIEW,
                        IamPermissionCodes.PLATFORM_TENANT_CREATE,
                        IamPermissionCodes.PLATFORM_TENANT_UPDATE,
                        IamPermissionCodes.PLATFORM_TENANT_STATUS,
                        IamPermissionCodes.PLATFORM_TENANT_CANCEL,
                        IamPermissionCodes.PLATFORM_TENANT_OWNER_ACTIVATE,
                        IamPermissionCodes.IAM_USER_PASSWORD_RESET,
                        IamPermissionCodes.PLATFORM_ORG_TEMPLATE_MANAGE,
                        IamPermissionCodes.PLATFORM_PERMISSION_MANAGE,
                        IamPermissionCodes.PLATFORM_AUDIT_VIEW,
                        IamPermissionCodes.PLATFORM_SMS_DELIVERY_VIEW,
                        IamPermissionCodes.PLATFORM_SMS_SETTINGS_VIEW,
                        IamPermissionCodes.PLATFORM_SMS_SETTINGS_UPDATE,
                        IamPermissionCodes.PLATFORM_SMS_SECURITY_VIEW,
                        IamPermissionCodes.PLATFORM_SMS_SECURITY_UPDATE
                );
        }
        AccountLifecycleMapper.TenantPermissionContextRow context = activeTenantContext(loginId);
        if (context == null) return List.of();
        if (!context.rootOwner()) {
            return roleMapper.selectEffectivePermissionCodes(context.tenantId(), context.accountId());
        }
        LinkedHashSet<String> permissions = new LinkedHashSet<>(List.of(
                        IamPermissionCodes.IAM_ORG_TYPE_VIEW,
                        IamPermissionCodes.IAM_ORG_TYPE_MANAGE,
                        IamPermissionCodes.IAM_ORG_VIEW,
                        IamPermissionCodes.IAM_ORG_CREATE,
                        IamPermissionCodes.IAM_ORG_UPDATE,
                        IamPermissionCodes.IAM_ORG_MOVE,
                        IamPermissionCodes.IAM_ORG_STATUS,
                        IamPermissionCodes.IAM_ORG_DELETE,
                        IamPermissionCodes.IAM_ROLE_VIEW,
                        IamPermissionCodes.IAM_ROLE_CREATE,
                        IamPermissionCodes.IAM_ROLE_UPDATE,
                        IamPermissionCodes.IAM_ROLE_STATUS,
                        IamPermissionCodes.IAM_ROLE_DELETE,
                        IamPermissionCodes.IAM_ROLE_GRANT,
                        IamPermissionCodes.IAM_ROLE_DATA_SCOPE,
                        IamPermissionCodes.IAM_USER_VIEW,
                        IamPermissionCodes.IAM_USER_CREATE,
                        IamPermissionCodes.IAM_USER_UPDATE,
                        IamPermissionCodes.IAM_USER_STATUS,
                        IamPermissionCodes.IAM_USER_CANCEL,
                        IamPermissionCodes.IAM_USER_ROLE_ASSIGN,
                        IamPermissionCodes.IAM_USER_PASSWORD_RESET,
                        IamPermissionCodes.IAM_AUDIT_VIEW
                ));
        roleMapper.selectGrantablePermissions(context.tenantId(), context.accountId(), true)
                .forEach(resource -> permissions.add(resource.permissionCode()));
        return List.copyOf(permissions);
    }

    /**
     * 返回当前真实有效身份拥有的角色编码。
     *
     * @param loginId Sa-Token 登录 ID
     * @param loginType Sa-Token 登录类型
     * @return 角色编码集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (activePlatformIdentity(loginId) != null) {
            return List.of(PLATFORM_SUPER_ADMIN_ROLE);
        }
        AccountLifecycleMapper.TenantPermissionContextRow context = activeTenantContext(loginId);
        if (context == null) return List.of();
        LinkedHashSet<String> roles = new LinkedHashSet<>(roleMapper.selectEffectiveRoleCodes(
                context.tenantId(), context.organizationId(), context.accountId()
        ));
        if (context.rootOwner()) roles.add(ORGANIZATION_OWNER_ROLE);
        return List.copyOf(roles);
    }

    /**
     * 解析并查询有效平台身份。
     *
     * @param loginId Sa-Token 登录 ID
     * @return 有效平台身份，不匹配时为空
     */
    private PlatformIdentityEntity activePlatformIdentity(Object loginId) {
        /** Sa-Token 登录 ID 文本。 */
        String value = String.valueOf(loginId);
        if (!value.startsWith(PLATFORM_LOGIN_PREFIX)) {
            return null;
        }
        /** 平台身份 ID 文本。 */
        String identityIdText = value.substring(PLATFORM_LOGIN_PREFIX.length());
        /** 平台身份 ID。 */
        long identityId;
        try {
            identityId = Long.parseLong(identityIdText);
        } catch (NumberFormatException exception) {
            return null;
        }
        /** 当前数据库平台身份。 */
        PlatformIdentityEntity identity = platformIdentityMapper.selectById(identityId);
        return identity != null
                && !Boolean.TRUE.equals(identity.getDeleted())
                && PlatformIdentityStatus.ACTIVE.name().equals(identity.getStatus())
                ? identity
                : null;
    }

    /**
     * 查询当前有效租户账号权限上下文。
     *
     * @param loginId Sa-Token 登录 ID
     * @return 有效租户账号上下文，不匹配时为空
     */
    private AccountLifecycleMapper.TenantPermissionContextRow activeTenantContext(Object loginId) {
        /** 租户登录 ID 文本。 */
        String value = String.valueOf(loginId);
        if (!value.startsWith(TENANT_LOGIN_PREFIX)) {
            return null;
        }
        /** 租户账号 ID。 */
        long accountId;
        try {
            accountId = Long.parseLong(value.substring(TENANT_LOGIN_PREFIX.length()));
        } catch (NumberFormatException exception) {
            return null;
        }
        /** 数据库最终校验后的租户账号上下文。 */
        AccountLifecycleMapper.TenantPermissionContextRow context =
                accountLifecycleMapper.selectTenantPermissionContext(accountId);
        return context != null
                && "ACTIVE".equals(context.accountStatus())
                && "ACTIVE".equals(context.tenantStatus())
                && "ACTIVE".equals(context.organizationStatus())
                ? context
                : null;
    }
}
