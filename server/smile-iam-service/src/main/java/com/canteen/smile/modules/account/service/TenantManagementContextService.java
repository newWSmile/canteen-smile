package com.canteen.smile.modules.account.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.modules.account.vo.TenantManagementContextVO;
import com.canteen.smile.modules.navigation.service.TenantNavigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 租户管理端启动上下文查询服务。 */
@Service
@RequiredArgsConstructor
public class TenantManagementContextService {

    /** 当前租户操作人解析服务。 */
    private final TenantActorService tenantActorService;
    /** 菜单显示与个人偏好查询服务。 */
    private final TenantNavigationService tenantNavigationService;

    /** @return 当前租户管理端真实身份和最终权限 */
    public TenantManagementContextVO current() {
        TenantActorContext actor = tenantActorService.current();
        return new TenantManagementContextVO(
                Long.toString(actor.accountId()),
                actor.username(),
                actor.displayName(),
                Long.toString(actor.tenantId()),
                actor.tenantName(),
                Long.toString(actor.organizationId()),
                actor.organizationName(),
                Long.toString(actor.rootOrganizationId()),
                actor.organizationOwner(),
                actor.rootOwner(),
                StpUtil.getPermissionList(),
                tenantNavigationService.hiddenMenuCodes(actor)
        );
    }
}
