package com.canteen.smile.modules.tenant.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.tenant.dto.TenantPageQuery;
import com.canteen.smile.modules.tenant.dto.CreateTenantRequest;
import com.canteen.smile.modules.tenant.dto.TenantOwnerPasswordResetRequest;
import com.canteen.smile.modules.tenant.dto.PlatformTenantStatusRequest;
import com.canteen.smile.modules.tenant.dto.UpdatePlatformTenantRequest;
import com.canteen.smile.modules.tenant.service.TenantCreationService;
import com.canteen.smile.modules.tenant.service.TenantQueryService;
import com.canteen.smile.modules.tenant.service.TenantOwnerActivationLinkService;
import com.canteen.smile.modules.tenant.service.TenantOwnerPasswordResetLinkService;
import com.canteen.smile.modules.tenant.service.PlatformTenantGovernanceService;
import com.canteen.smile.modules.tenant.vo.TenantSummaryVO;
import com.canteen.smile.modules.tenant.vo.TenantCreationVO;
import com.canteen.smile.modules.tenant.vo.TenantOwnerActivationLinkVO;
import com.canteen.smile.modules.tenant.vo.TenantOwnerPasswordResetLinkVO;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/** 平台超级管理员租户查询接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.PLATFORM_TENANTS)
public class PlatformTenantController {

    /** 租户查询服务。 */
    private final TenantQueryService tenantQueryService;

    /** 租户创建编排服务。 */
    private final TenantCreationService tenantCreationService;

    /** 租户首位所有者激活链接服务。 */
    private final TenantOwnerActivationLinkService ownerActivationLinkService;

    /** 租户所有者密码恢复链接服务。 */
    private final TenantOwnerPasswordResetLinkService ownerPasswordResetLinkService;

    /** 平台租户资料与生命周期治理服务。 */
    private final PlatformTenantGovernanceService tenantGovernanceService;

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

    /**
     * 创建租户、根机构、首位所有者及待激活 Auth 凭证。
     *
     * @param idempotencyKey 客户端为本次创建命令生成的稳定幂等键
     * @param request 完整五步初始化参数
     * @return 创建后的租户和所有者状态
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission(IamPermissionCodes.PLATFORM_TENANT_CREATE)
    public ApiResponse<TenantCreationVO> createTenant(
            @RequestHeader("Idempotency-Key")
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 128) String idempotencyKey,
            @Valid @RequestBody CreateTenantRequest request
    ) {
        return ApiResponse.success(tenantCreationService.create(request, idempotencyKey));
    }

    /**
     * 为仍处于待激活状态的租户根机构所有者签发新的一次性激活票据。
     *
     * @param tenantId 租户 ID
     * @return 只展示一次的激活信息
     */
    @PostMapping(IamApiPaths.PLATFORM_TENANT_OWNER_ACTIVATION_LINKS)
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission(IamPermissionCodes.PLATFORM_TENANT_OWNER_ACTIVATE)
    public ApiResponse<TenantOwnerActivationLinkVO> issueOwnerActivationLink(
            @Positive @PathVariable long tenantId
    ) {
        return ApiResponse.success(ownerActivationLinkService.issue(tenantId));
    }

    /**
     * 为已经激活的租户根机构所有者签发 30 分钟一次性密码恢复票据。
     *
     * @param tenantId 租户 ID
     * @param request 再认证票据和操作原因
     * @return 只展示一次的密码恢复信息
     */
    @PostMapping(IamApiPaths.PLATFORM_TENANT_OWNER_PASSWORD_RESET_LINKS)
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission(IamPermissionCodes.IAM_USER_PASSWORD_RESET)
    public ApiResponse<TenantOwnerPasswordResetLinkVO> issueOwnerPasswordResetLink(
            @Positive @PathVariable long tenantId,
            @Valid @RequestBody TenantOwnerPasswordResetRequest request
    ) {
        return ApiResponse.success(ownerPasswordResetLinkService.issue(tenantId, request));
    }

    /** 修改租户可变基础资料。 */
    @PutMapping("/{tenantId}")
    @SaCheckPermission(IamPermissionCodes.PLATFORM_TENANT_UPDATE)
    public ApiResponse<TenantSummaryVO> updateTenant(
            @Positive @PathVariable long tenantId,
            @Valid @RequestBody UpdatePlatformTenantRequest request
    ) {
        return ApiResponse.success(tenantGovernanceService.update(tenantId, request));
    }

    /** 暂停正常租户。 */
    @PostMapping("/{tenantId}/actions/suspend")
    @SaCheckPermission(IamPermissionCodes.PLATFORM_TENANT_STATUS)
    public ApiResponse<TenantSummaryVO> suspendTenant(
            @Positive @PathVariable long tenantId,
            @Valid @RequestBody PlatformTenantStatusRequest request
    ) {
        return ApiResponse.success(tenantGovernanceService.suspend(tenantId, request));
    }

    /** 恢复已暂停或已到期租户。 */
    @PostMapping("/{tenantId}/actions/resume")
    @SaCheckPermission(IamPermissionCodes.PLATFORM_TENANT_STATUS)
    public ApiResponse<TenantSummaryVO> resumeTenant(
            @Positive @PathVariable long tenantId,
            @Valid @RequestBody PlatformTenantStatusRequest request
    ) {
        return ApiResponse.success(tenantGovernanceService.resume(tenantId, request));
    }

    /** 不可恢复地注销租户。 */
    @PostMapping("/{tenantId}/actions/cancel")
    @SaCheckPermission(IamPermissionCodes.PLATFORM_TENANT_CANCEL)
    public ApiResponse<TenantSummaryVO> cancelTenant(
            @Positive @PathVariable long tenantId,
            @Valid @RequestBody PlatformTenantStatusRequest request
    ) {
        return ApiResponse.success(tenantGovernanceService.cancel(tenantId, request));
    }
}
