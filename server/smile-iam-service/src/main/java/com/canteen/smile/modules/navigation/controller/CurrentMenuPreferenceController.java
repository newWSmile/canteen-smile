package com.canteen.smile.modules.navigation.controller;

import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.navigation.dto.UpdateMenuPreferenceRequest;
import com.canteen.smile.modules.navigation.service.TenantNavigationService;
import com.canteen.smile.modules.navigation.vo.TenantMenuSettingVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前账号维护个人菜单隐藏偏好的接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.CURRENT_MENU_PREFERENCES)
public class CurrentMenuPreferenceController {

    /** 租户导航治理服务。 */
    private final TenantNavigationService service;

    /** @return 当前账号可以自行隐藏的菜单列表。 */
    @GetMapping
    public ApiResponse<List<TenantMenuSettingVO>> preferences() {
        return ApiResponse.success(service.preferences());
    }

    /** 修改指定菜单的个人隐藏偏好。 */
    @PutMapping("/{permissionCode}")
    public ApiResponse<List<TenantMenuSettingVO>> update(
            @PathVariable @NotBlank String permissionCode,
            @Valid @RequestBody UpdateMenuPreferenceRequest request
    ) {
        return ApiResponse.success(service.updatePreference(permissionCode, request));
    }
}
