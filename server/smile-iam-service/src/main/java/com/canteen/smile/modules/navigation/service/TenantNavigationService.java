package com.canteen.smile.modules.navigation.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.navigation.dto.UpdateMenuPreferenceRequest;
import com.canteen.smile.modules.navigation.dto.UpdateTenantFeatureRequest;
import com.canteen.smile.modules.navigation.dto.UpdateTenantMenuVisibilityRequest;
import com.canteen.smile.modules.navigation.mapper.TenantNavigationMapper;
import com.canteen.smile.modules.navigation.vo.TenantFeatureVO;
import com.canteen.smile.modules.navigation.vo.TenantMenuSettingVO;
import com.canteen.smile.modules.navigation.vo.TenantNavigationSettingsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 租户导航治理查询与跨服务再认证编排服务。 */
@Service
@RequiredArgsConstructor
public class TenantNavigationService {

    /** 当前租户操作人服务。 */
    private final TenantActorService actorService;
    /** 导航配置数据访问接口。 */
    private final TenantNavigationMapper mapper;
    /** 本地事务命令服务。 */
    private final TenantNavigationCommandService commandService;
    /** Auth 内部调用客户端。 */
    private final AuthTenantAccountClient authClient;

    /** @return 租户功能和统一菜单配置。 */
    @Transactional(readOnly = true)
    public TenantNavigationSettingsVO settings() {
        TenantActorContext actor = actorService.current();
        return assemble(actor);
    }

    /** @return 当前账号可配置的个人菜单偏好。 */
    @Transactional(readOnly = true)
    public List<TenantMenuSettingVO> preferences() {
        TenantActorContext actor = actorService.current();
        return selectPreferenceMenus(actor);
    }

    /** 修改功能开关并返回最新配置。 */
    public TenantNavigationSettingsVO updateFeature(String featureCode, UpdateTenantFeatureRequest request) {
        TenantActorContext actor = actorService.current();
        TenantFeatureCatalog feature = requireFeature(featureCode);
        authClient.consumeTenantReauthTicket(actor.accountId(), request.reauthTicket(),
                "TENANT_NAVIGATION_UPDATE");
        commandService.updateFeature(actor, feature.code(), request);
        return assemble(actor);
    }

    /** 修改统一菜单显示并返回最新配置。 */
    public TenantNavigationSettingsVO updateTenantMenu(String permissionCode,
                                                        UpdateTenantMenuVisibilityRequest request) {
        TenantActorContext actor = actorService.current();
        requireMenu(actor, permissionCode);
        authClient.consumeTenantReauthTicket(actor.accountId(), request.reauthTicket(),
                "TENANT_NAVIGATION_UPDATE");
        commandService.updateTenantMenu(actor, permissionCode, request);
        return assemble(actor);
    }

    /** 修改当前账号个人菜单偏好并返回最新列表。 */
    public List<TenantMenuSettingVO> updatePreference(String permissionCode,
                                                      UpdateMenuPreferenceRequest request) {
        TenantActorContext actor = actorService.current();
        TenantNavigationMapper.MenuRow menu = requirePreferenceMenu(actor, permissionCode);
        commandService.updatePreference(actor, permissionCode, request, menu.preferenceVersion());
        return selectPreferenceMenus(actor);
    }

    /** @return 当前账号有效隐藏菜单权限码。 */
    @Transactional(readOnly = true)
    public List<String> hiddenMenuCodes(TenantActorContext actor) {
        List<String> permissionCodes = List.copyOf(StpUtil.getPermissionList());
        if (permissionCodes.isEmpty()) return List.of();
        return mapper.selectEffectiveHiddenMenuCodes(actor.tenantId(), actor.accountId(), permissionCodes);
    }

    /** 组装完整配置。 */
    private TenantNavigationSettingsVO assemble(TenantActorContext actor) {
        List<TenantFeatureVO> features = mapper.selectFeatures(actor.tenantId()).stream().map(row -> {
            TenantFeatureCatalog feature = requireFeature(row.featureCode());
            return new TenantFeatureVO(row.featureCode(), feature.displayName(), feature.description(),
                    row.enabled(), row.version());
        }).toList();
        return new TenantNavigationSettingsVO(features,
                assembleMenus(mapper.selectMenus(actor.tenantId(), actor.accountId())));
    }

    /** 将菜单 ID 父子关系转换为稳定权限码关系。 */
    private List<TenantMenuSettingVO> assembleMenus(List<TenantNavigationMapper.MenuRow> rows) {
        Map<Long, String> permissionCodes = new HashMap<>();
        rows.forEach(row -> permissionCodes.put(row.permissionId(), row.permissionCode()));
        return rows.stream().map(row -> new TenantMenuSettingVO(
                row.permissionCode(), row.parentId() == null ? null : permissionCodes.get(row.parentId()),
                row.name(), row.routePath(), row.featureCode(), row.featureEnabled(), row.tenantHidden(),
                row.tenantVersion(), row.personallyHidden(), row.preferenceVersion() == null ? 0L
                        : row.preferenceVersion(), row.sortOrder()
        )).toList();
    }

    /** @return 已知功能目录。 */
    private TenantFeatureCatalog requireFeature(String featureCode) {
        TenantFeatureCatalog feature = TenantFeatureCatalog.find(featureCode);
        if (feature == null) throw new BusinessException("IAM_2901", "功能编码不存在或不可维护", 404);
        return feature;
    }

    /** @return 当前租户已发布菜单。 */
    private TenantNavigationMapper.MenuRow requireMenu(TenantActorContext actor, String permissionCode) {
        return mapper.selectMenus(actor.tenantId(), actor.accountId()).stream()
                .filter(row -> row.permissionCode().equals(permissionCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException("IAM_2902", "菜单不存在或不可配置", 404));
    }

    /** @return 当前账号确实可访问并可维护个人偏好的菜单。 */
    private TenantNavigationMapper.MenuRow requirePreferenceMenu(TenantActorContext actor,
                                                                  String permissionCode) {
        return preferenceMenuRows(actor).stream()
                .filter(row -> row.permissionCode().equals(permissionCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException("IAM_2903", "菜单未授权、已停用或已被租户隐藏", 404));
    }

    /** @return 当前账号有效权限范围内的个人偏好菜单。 */
    private List<TenantMenuSettingVO> selectPreferenceMenus(TenantActorContext actor) {
        return assembleMenus(preferenceMenuRows(actor));
    }

    /** @return 当前账号有效权限与租户导航策略共同过滤后的菜单数据行。 */
    private List<TenantNavigationMapper.MenuRow> preferenceMenuRows(TenantActorContext actor) {
        List<String> permissionCodes = List.copyOf(StpUtil.getPermissionList());
        if (permissionCodes.isEmpty()) return List.of();
        return mapper.selectPreferenceMenus(actor.tenantId(), actor.accountId(), permissionCodes);
    }
}
