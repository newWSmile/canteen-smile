package com.canteen.smile.modules.organization.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.organization.dto.ChangeOrganizationTypeStatusRequest;
import com.canteen.smile.modules.organization.dto.CreateOrganizationTypeRequest;
import com.canteen.smile.modules.organization.dto.OrganizationTypePageQuery;
import com.canteen.smile.modules.organization.dto.UpdateOrganizationTypeRequest;
import com.canteen.smile.modules.organization.service.TenantOrganizationTypeService;
import com.canteen.smile.modules.organization.vo.OrganizationTypeVO;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 租户独立机构类型管理接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.TENANT_ORGANIZATION_TYPES)
public class TenantOrganizationTypeController {

    /** 机构类型领域服务。 */
    private final TenantOrganizationTypeService service;

    /** @param query 分页条件 @return 当前租户机构类型分页 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_TYPE_VIEW)
    public ApiResponse<PageResult<OrganizationTypeVO>> page(
            @Valid @ModelAttribute OrganizationTypePageQuery query
    ) {
        return ApiResponse.success(service.page(query));
    }

    /** @return 当前租户全部有效机构类型 */
    @GetMapping("/active")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_TYPE_VIEW)
    public ApiResponse<List<OrganizationTypeVO>> activeTypes() {
        return ApiResponse.success(service.activeTypes());
    }

    /** @param request 新增参数 @return 新增类型 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_TYPE_MANAGE)
    public ApiResponse<OrganizationTypeVO> create(
            @Valid @RequestBody CreateOrganizationTypeRequest request
    ) {
        return ApiResponse.success(service.create(request));
    }

    /** @param typeId 类型 ID @param request 修改参数 @return 修改后的类型 */
    @PutMapping("/{typeId}")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_TYPE_MANAGE)
    public ApiResponse<OrganizationTypeVO> update(
            @Positive @PathVariable long typeId,
            @Valid @RequestBody UpdateOrganizationTypeRequest request
    ) {
        return ApiResponse.success(service.update(typeId, request));
    }

    /** @param typeId 类型 ID @param request 状态参数 @return 修改后的类型 */
    @PutMapping("/{typeId}/status")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_TYPE_MANAGE)
    public ApiResponse<OrganizationTypeVO> changeStatus(
            @Positive @PathVariable long typeId,
            @Valid @RequestBody ChangeOrganizationTypeStatusRequest request
    ) {
        return ApiResponse.success(service.changeStatus(typeId, request));
    }
}
