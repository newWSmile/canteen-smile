package com.canteen.smile.modules.navigation.service;

import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.audit.spi.AuditClientIpResolver;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.navigation.dto.UpdateMenuPreferenceRequest;
import com.canteen.smile.modules.navigation.dto.UpdateTenantFeatureRequest;
import com.canteen.smile.modules.navigation.dto.UpdateTenantMenuVisibilityRequest;
import com.canteen.smile.modules.navigation.mapper.TenantNavigationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 租户功能、统一菜单和个人菜单偏好的本地事务服务。 */
@Service
@RequiredArgsConstructor
public class TenantNavigationCommandService {

    /** 导航配置数据访问接口。 */
    private final TenantNavigationMapper mapper;
    /** 当前请求客户端 IP 解析器。 */
    private final AuditClientIpResolver clientIpResolver;

    /**
     * 修改功能开关并让租户全部账号重新登录，以刷新最终权限。
     *
     * @param actor 已通过功能与菜单管理权限校验的租户操作人
     * @param featureCode 稳定功能编码
     * @param request 修改命令
     */
    @Transactional
    @AuditOperation(
            source = "IAM", categoryPath = {"租户端", "租户治理", "功能启停"},
            actionCode = "iam:tenant-navigation:feature-update", actionName = "调整租户功能状态",
            targetType = "TENANT_FEATURE", targetId = "#featureCode", targetCode = "#featureCode",
            reason = "#request.reason"
    )
    public void updateFeature(TenantActorContext actor, String featureCode, UpdateTenantFeatureRequest request) {
        if (mapper.updateFeature(actor.tenantId(), featureCode, request.enabled(), request.version(),
                actor.accountId()) != 1) {
            throw concurrentChange();
        }
        mapper.bumpTenantAccountAuthzVersions(actor.tenantId(), actor.accountId());
        mapper.insertTenantSessionInvalidationOutbox(actor.tenantId(), actor.accountId(),
                "租户功能状态已调整，强制重新登录刷新权限", clientIpResolver.resolve());
    }

    /**
     * 修改租户统一菜单隐藏状态；该操作不改变接口权限。
     *
     * @param actor 已通过功能与菜单管理权限校验的租户操作人
     * @param permissionCode 菜单权限码
     * @param request 修改命令
     */
    @Transactional
    @AuditOperation(
            source = "IAM", categoryPath = {"租户端", "租户治理", "菜单显示"},
            actionCode = "iam:tenant-navigation:menu-update", actionName = "调整租户菜单显示",
            targetType = "MENU_PERMISSION", targetId = "#permissionCode", targetCode = "#permissionCode",
            reason = "#request.reason"
    )
    public void updateTenantMenu(TenantActorContext actor, String permissionCode,
                                 UpdateTenantMenuVisibilityRequest request) {
        if (mapper.updateTenantMenu(actor.tenantId(), permissionCode, request.hidden(), request.version(),
                actor.accountId()) != 1) {
            throw concurrentChange();
        }
    }

    /**
     * 新增或修改当前账号个人菜单偏好。
     *
     * @param actor 当前账号
     * @param permissionCode 菜单权限码
     * @param request 修改命令
     * @param existingVersion 数据库现有版本；不存在时为空
     */
    @Transactional
    @AuditOperation(
            source = "IAM", categoryPath = {"租户端", "个人设置", "菜单偏好"},
            actionCode = "iam:me:menu-preference", actionName = "调整个人菜单偏好",
            targetType = "MENU_PERMISSION", targetId = "#permissionCode", targetCode = "#permissionCode"
    )
    public void updatePreference(TenantActorContext actor, String permissionCode,
                                 UpdateMenuPreferenceRequest request, Long existingVersion) {
        if (existingVersion == null) {
            if (request.version() != 0 || mapper.insertPreference(actor.tenantId(), actor.organizationId(),
                    actor.accountId(), permissionCode, request.hidden()) != 1) {
                throw concurrentChange();
            }
            return;
        }
        if (!existingVersion.equals(request.version()) || mapper.updatePreference(actor.tenantId(),
                actor.organizationId(), actor.accountId(), permissionCode, request.hidden(),
                request.version()) != 1) {
            throw concurrentChange();
        }
    }

    /** @return 配置已被并发修改异常。 */
    private BusinessException concurrentChange() {
        return new BusinessException("IAM_2904", "配置已发生变化，请刷新后重试", 409);
    }
}
