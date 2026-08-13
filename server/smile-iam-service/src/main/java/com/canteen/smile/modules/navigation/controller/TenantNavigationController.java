package com.canteen.smile.modules.navigation.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.navigation.dto.UpdateTenantFeatureRequest;
import com.canteen.smile.modules.navigation.dto.UpdateTenantMenuVisibilityRequest;
import com.canteen.smile.modules.navigation.service.TenantNavigationService;
import com.canteen.smile.modules.navigation.vo.TenantNavigationSettingsVO;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 具备对应权限的租户管理员维护功能启停和统一菜单显示的接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.TENANT_NAVIGATION_SETTINGS)
public class TenantNavigationController {

    /** 租户导航治理服务。 */
    private final TenantNavigationService service;

    /** @return 当前租户全部功能和菜单配置。 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.IAM_TENANT_NAVIGATION_VIEW)
    public ApiResponse<TenantNavigationSettingsVO> settings() {
        return ApiResponse.success(service.settings());
    }

    /** 修改指定功能开关。 */
    @PutMapping("/features/{featureCode}")
    @SaCheckPermission(IamPermissionCodes.IAM_TENANT_NAVIGATION_MANAGE)
    public ApiResponse<TenantNavigationSettingsVO> updateFeature(
            @PathVariable @Pattern(regexp = "[A-Z][A-Z0-9_]{2,127}") String featureCode,
            @Valid @RequestBody UpdateTenantFeatureRequest request
    ) {
        return ApiResponse.success(service.updateFeature(featureCode, request));
    }

    /** 修改指定菜单对租户全部机构的显示状态。 */
    @PutMapping("/menus/{permissionCode}")
    @SaCheckPermission(IamPermissionCodes.IAM_TENANT_NAVIGATION_MANAGE)
    public ApiResponse<TenantNavigationSettingsVO> updateMenu(
            @PathVariable @NotBlank String permissionCode,
            @Valid @RequestBody UpdateTenantMenuVisibilityRequest request
    ) {
        return ApiResponse.success(service.updateTenantMenu(permissionCode, request));
    }
}
