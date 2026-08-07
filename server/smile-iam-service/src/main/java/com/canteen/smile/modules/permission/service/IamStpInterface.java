package com.canteen.smile.modules.permission.service;

import cn.dev33.satoken.stp.StpInterface;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.platform.entity.PlatformIdentityEntity;
import com.canteen.smile.modules.platform.mapper.PlatformIdentityMapper;
import com.canteen.smile.modules.platform.model.PlatformIdentityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** IAM 服务基于真实身份状态提供 Sa-Token 最终权限与角色集合。 */
@Component
@RequiredArgsConstructor
public class IamStpInterface implements StpInterface {

    /** 平台登录 ID 前缀。 */
    private static final String PLATFORM_LOGIN_PREFIX = "PLATFORM:";

    /** 平台超级管理员角色编码。 */
    private static final String PLATFORM_SUPER_ADMIN_ROLE = "PLATFORM_SUPER_ADMIN";

    /** 平台身份数据访问接口。 */
    private final PlatformIdentityMapper platformIdentityMapper;

    /**
     * 返回当前真实有效身份拥有的权限码。
     *
     * @param loginId Sa-Token 登录 ID
     * @param loginType Sa-Token 登录类型
     * @return 权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return activePlatformIdentity(loginId) == null
                ? List.of()
                : List.of(IamPermissionCodes.PLATFORM_TENANT_VIEW);
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
        return activePlatformIdentity(loginId) == null
                ? List.of()
                : List.of(PLATFORM_SUPER_ADMIN_ROLE);
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
}
