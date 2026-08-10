package com.canteen.smile.modules.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.role.dto.CreateRoleRequest;
import com.canteen.smile.modules.role.dto.ReplaceRoleDataPolicyRequest;
import com.canteen.smile.modules.role.dto.ReplaceRolePermissionsRequest;
import com.canteen.smile.modules.role.dto.RolePageQuery;
import com.canteen.smile.modules.role.dto.RoleStatusRequest;
import com.canteen.smile.modules.role.dto.UpdateRoleRequest;
import com.canteen.smile.modules.role.service.RoleService;
import com.canteen.smile.modules.role.vo.GrantBoundaryVO;
import com.canteen.smile.modules.role.vo.RoleDataPolicyVO;
import com.canteen.smile.modules.role.vo.RolePermissionVO;
import com.canteen.smile.modules.role.vo.RoleVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 租户当前机构角色、权限和数据范围接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.EXTERNAL_V1 + "/tenant")
public class TenantRoleController {

    /** 角色领域服务。 */
    private final RoleService service;

    /** @param query 分页条件 @return 当前机构角色分页 */
    @GetMapping("/roles")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_VIEW)
    public ApiResponse<PageResult<RoleVO>> page(@Valid @ModelAttribute RolePageQuery query) {
        return ApiResponse.success(service.page(query));
    }

    /** @param roleId 角色 ID @return 角色详情 */
    @GetMapping("/roles/{roleId}")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_VIEW)
    public ApiResponse<RoleVO> detail(@Positive @PathVariable long roleId) {
        return ApiResponse.success(service.detail(roleId));
    }

    /** @param request 新建参数 @return 新建角色 */
    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_CREATE)
    public ApiResponse<RoleVO> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.success(service.create(request));
    }

    /** @param roleId 角色 ID @param request 修改参数 @return 修改后的角色 */
    @PutMapping("/roles/{roleId}")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_UPDATE)
    public ApiResponse<RoleVO> update(@Positive @PathVariable long roleId,
                                      @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success(service.update(roleId, request));
    }

    /** @param roleId 角色 ID @param request 状态命令 @return 停用后的角色 */
    @PostMapping("/roles/{roleId}/actions/disable")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_STATUS)
    public ApiResponse<RoleVO> disable(@Positive @PathVariable long roleId,
                                       @Valid @RequestBody RoleStatusRequest request) {
        return ApiResponse.success(service.changeStatus(roleId, false, request));
    }

    /** @param roleId 角色 ID @param request 状态命令 @return 启用后的角色 */
    @PostMapping("/roles/{roleId}/actions/enable")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_STATUS)
    public ApiResponse<RoleVO> enable(@Positive @PathVariable long roleId,
                                      @Valid @RequestBody RoleStatusRequest request) {
        return ApiResponse.success(service.changeStatus(roleId, true, request));
    }

    /** @param roleId 角色 ID @param request 删除命令 */
    @PostMapping("/roles/{roleId}/actions/delete")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_DELETE)
    public ApiResponse<Void> delete(@Positive @PathVariable long roleId,
                                    @Valid @RequestBody RoleStatusRequest request) {
        service.delete(roleId, request);
        return ApiResponse.success(null);
    }

    /** @return 当前操作者可授予上限 */
    @GetMapping("/grant-boundary")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_VIEW)
    public ApiResponse<GrantBoundaryVO> grantBoundary() {
        return ApiResponse.success(service.grantBoundary());
    }

    /** @param roleId 可选角色 ID @return 可分配权限树 */
    @GetMapping("/permission-tree")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_VIEW)
    public ApiResponse<List<RolePermissionVO>> permissionTree(
            @Positive @RequestParam(required = false) Long roleId
    ) {
        return ApiResponse.success(service.permissionTree(roleId));
    }

    /** @param roleId 角色 ID @return 角色权限树 */
    @GetMapping("/roles/{roleId}/permissions")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_VIEW)
    public ApiResponse<List<RolePermissionVO>> permissions(@Positive @PathVariable long roleId) {
        return ApiResponse.success(service.permissionTree(roleId));
    }

    /** @param roleId 角色 ID @param request 完整权限集合 @return 保存后的权限树 */
    @PutMapping("/roles/{roleId}/permissions")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_GRANT)
    public ApiResponse<List<RolePermissionVO>> replacePermissions(
            @Positive @PathVariable long roleId,
            @Valid @RequestBody ReplaceRolePermissionsRequest request
    ) {
        return ApiResponse.success(service.replacePermissions(roleId, request));
    }

    /** @param roleId 角色 ID @return 角色数据策略 */
    @GetMapping("/roles/{roleId}/data-policy")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_VIEW)
    public ApiResponse<List<RoleDataPolicyVO>> dataPolicies(@Positive @PathVariable long roleId) {
        return ApiResponse.success(service.dataPolicies(roleId));
    }

    /** @param roleId 角色 ID @param request 完整数据策略 @return 保存后的策略 */
    @PutMapping("/roles/{roleId}/data-policy")
    @SaCheckPermission(IamPermissionCodes.IAM_ROLE_DATA_SCOPE)
    public ApiResponse<List<RoleDataPolicyVO>> replaceDataPolicies(
            @Positive @PathVariable long roleId,
            @Valid @RequestBody ReplaceRoleDataPolicyRequest request
    ) {
        return ApiResponse.success(service.replaceDataPolicies(roleId, request));
    }
}
