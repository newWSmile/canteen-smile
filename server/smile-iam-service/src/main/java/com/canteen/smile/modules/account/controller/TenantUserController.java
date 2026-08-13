package com.canteen.smile.modules.account.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.account.dto.CreateTenantUserRequest;
import com.canteen.smile.modules.account.dto.ReplaceTenantUserRolesRequest;
import com.canteen.smile.modules.account.dto.TenantUserPageQuery;
import com.canteen.smile.modules.account.dto.TenantUserStatusRequest;
import com.canteen.smile.modules.account.dto.TenantUserPasswordResetRequest;
import com.canteen.smile.modules.account.dto.UpdateTenantUserRequest;
import com.canteen.smile.modules.account.service.TenantUserService;
import com.canteen.smile.modules.account.vo.TenantUserActivationLinkVO;
import com.canteen.smile.modules.account.vo.TenantUserVO;
import com.canteen.smile.modules.account.vo.TenantUserPasswordResetLinkVO;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 租户当前机构用户和角色分配接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.TENANT_USERS)
public class TenantUserController {
    /** 用户领域服务。 */
    private final TenantUserService service;

    /** @param query 分页条件 @return 本机构用户分页 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.IAM_USER_VIEW)
    public ApiResponse<PageResult<TenantUserVO>> page(@Valid @ModelAttribute TenantUserPageQuery query) {
        return ApiResponse.success(service.page(query));
    }

    /** @param accountId 账号 ID @return 用户详情 */
    @GetMapping("/{accountId}")
    @SaCheckPermission(IamPermissionCodes.IAM_USER_VIEW)
    public ApiResponse<TenantUserVO> detail(@Positive @PathVariable long accountId) {
        return ApiResponse.success(service.detail(accountId));
    }

    /** @param request 创建请求 @return 待激活用户 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission(IamPermissionCodes.IAM_USER_CREATE)
    public ApiResponse<TenantUserVO> create(@Valid @RequestBody CreateTenantUserRequest request) {
        return ApiResponse.success(service.create(request));
    }

    /** @param accountId 账号 ID @param request 角色替换请求 @return 更新后的用户 */
    @PutMapping("/{accountId}/roles")
    @SaCheckPermission(IamPermissionCodes.IAM_USER_ROLE_ASSIGN)
    public ApiResponse<TenantUserVO> replaceRoles(@Positive @PathVariable long accountId,
                                                   @Valid @RequestBody ReplaceTenantUserRolesRequest request) {
        return ApiResponse.success(service.replaceRoles(accountId, request));
    }

    /** @param accountId 账号 ID @param request 资料修改请求 @return 更新后用户 */
    @PatchMapping("/{accountId}")
    @SaCheckPermission(IamPermissionCodes.IAM_USER_UPDATE)
    public ApiResponse<TenantUserVO> update(@Positive @PathVariable long accountId,
                                            @Valid @RequestBody UpdateTenantUserRequest request) {
        return ApiResponse.success(service.update(accountId, request));
    }

    /** @param accountId 账号 ID @param request 停用命令 @return 更新后用户 */
    @PostMapping("/{accountId}/actions/disable")
    @SaCheckPermission(IamPermissionCodes.IAM_USER_STATUS)
    public ApiResponse<TenantUserVO> disable(@Positive @PathVariable long accountId,
                                             @Valid @RequestBody TenantUserStatusRequest request) {
        return ApiResponse.success(service.changeStatus(accountId, request, false));
    }

    /** @param accountId 账号 ID @param request 恢复命令 @return 更新后用户 */
    @PostMapping("/{accountId}/actions/enable")
    @SaCheckPermission(IamPermissionCodes.IAM_USER_STATUS)
    public ApiResponse<TenantUserVO> enable(@Positive @PathVariable long accountId,
                                            @Valid @RequestBody TenantUserStatusRequest request) {
        return ApiResponse.success(service.changeStatus(accountId, request, true));
    }

    /** @param accountId 账号 ID @param request 不可恢复注销命令 @return 空成功响应 */
    @PostMapping("/{accountId}/actions/cancel")
    @SaCheckPermission(IamPermissionCodes.IAM_USER_CANCEL)
    public ApiResponse<Void> cancel(@Positive @PathVariable long accountId,
                                    @Valid @RequestBody TenantUserStatusRequest request) {
        service.cancel(accountId, request);
        return ApiResponse.success(null);
    }

    /** @param accountId 待激活账号 ID @return 只展示一次的激活票据 */
    @PostMapping("/{accountId}/activation-links")
    @SaCheckPermission(IamPermissionCodes.IAM_USER_CREATE)
    public ApiResponse<TenantUserActivationLinkVO> issueActivationLink(@Positive @PathVariable long accountId) {
        return ApiResponse.success(service.issueActivationLink(accountId));
    }

    /** @param accountId 目标账号 ID @param request 密码重置请求 @return 一次性重置票据 */
    @PostMapping("/{accountId}/password-reset-links")
    @SaCheckPermission(IamPermissionCodes.IAM_USER_PASSWORD_RESET)
    public ApiResponse<TenantUserPasswordResetLinkVO> issuePasswordResetLink(
            @Positive @PathVariable long accountId,
            @Valid @RequestBody TenantUserPasswordResetRequest request
    ) {
        return ApiResponse.success(service.issuePasswordResetLink(accountId, request));
    }
}
