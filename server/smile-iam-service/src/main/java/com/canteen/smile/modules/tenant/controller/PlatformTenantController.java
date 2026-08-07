package com.canteen.smile.modules.tenant.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.tenant.dto.TenantPageQuery;
import com.canteen.smile.modules.tenant.service.TenantQueryService;
import com.canteen.smile.modules.tenant.vo.TenantSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 平台超级管理员租户查询接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.PLATFORM_TENANTS)
public class PlatformTenantController {

    /** 租户查询服务。 */
    private final TenantQueryService tenantQueryService;

    /**
     * 分页查询平台管理范围内的租户。
     *
     * @param query 分页和状态过滤条件
     * @return 统一租户分页响应
     */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.PLATFORM_TENANT_VIEW)
    public ApiResponse<PageResult<TenantSummaryVO>> pageTenants(
            @Valid @ModelAttribute TenantPageQuery query
    ) {
        return ApiResponse.success(tenantQueryService.pageTenants(query));
    }
}
