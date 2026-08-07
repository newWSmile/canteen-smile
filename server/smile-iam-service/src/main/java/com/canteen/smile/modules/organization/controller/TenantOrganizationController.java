package com.canteen.smile.modules.organization.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.organization.dto.ChangeOrganizationStatusRequest;
import com.canteen.smile.modules.organization.dto.CreateOrganizationRequest;
import com.canteen.smile.modules.organization.dto.DeleteOrganizationRequest;
import com.canteen.smile.modules.organization.dto.MoveOrganizationRequest;
import com.canteen.smile.modules.organization.dto.OrganizationPageQuery;
import com.canteen.smile.modules.organization.dto.UpdateOrganizationRequest;
import com.canteen.smile.modules.organization.service.TenantOrganizationService;
import com.canteen.smile.modules.organization.vo.OrganizationSearchVO;
import com.canteen.smile.modules.organization.vo.OrganizationVO;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** 租户机构树管理接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.TENANT_ORGANIZATIONS)
public class TenantOrganizationController {

    /** 机构树领域服务。 */
    private final TenantOrganizationService service;

    /** @return 当前租户根机构 */
    @GetMapping("/root")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_VIEW)
    public ApiResponse<OrganizationVO> root() {
        return ApiResponse.success(service.root());
    }

    /** @param query 直属子机构分页条件 @return 当前页机构 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_VIEW)
    public ApiResponse<PageResult<OrganizationVO>> children(
            @Valid @ModelAttribute OrganizationPageQuery query
    ) {
        return ApiResponse.success(service.children(query));
    }

    /** @param keyword 名称或业务编码关键词 @return 有界搜索结果 */
    @GetMapping("/search")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_VIEW)
    public ApiResponse<List<OrganizationSearchVO>> search(
            @NotBlank @Size(min = 2, max = 200) String keyword
    ) {
        return ApiResponse.success(service.search(keyword));
    }

    /** @param organizationId 机构 ID @return 机构详情 */
    @GetMapping("/{organizationId}")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_VIEW)
    public ApiResponse<OrganizationVO> detail(@Positive @PathVariable long organizationId) {
        return ApiResponse.success(service.detail(organizationId));
    }

    /** @param request 新机构参数 @return 新增机构 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_CREATE)
    public ApiResponse<OrganizationVO> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ApiResponse.success(service.create(request));
    }

    /** @param organizationId 机构 ID @param request 修改参数 @return 修改后的机构 */
    @PutMapping("/{organizationId}")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_UPDATE)
    public ApiResponse<OrganizationVO> update(
            @Positive @PathVariable long organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return ApiResponse.success(service.update(organizationId, request));
    }

    /** @param organizationId 机构 ID @param request 迁移参数 @return 迁移后的机构 */
    @PutMapping("/{organizationId}/parent")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_MOVE)
    public ApiResponse<OrganizationVO> move(
            @Positive @PathVariable long organizationId,
            @Valid @RequestBody MoveOrganizationRequest request
    ) {
        return ApiResponse.success(service.move(organizationId, request));
    }

    /** @param organizationId 机构 ID @param request 状态参数 @return 修改后的机构 */
    @PutMapping("/{organizationId}/status")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_STATUS)
    public ApiResponse<OrganizationVO> changeStatus(
            @Positive @PathVariable long organizationId,
            @Valid @RequestBody ChangeOrganizationStatusRequest request
    ) {
        return ApiResponse.success(service.changeStatus(organizationId, request));
    }

    /** @param organizationId 机构 ID @param request 删除原因 */
    @DeleteMapping("/{organizationId}")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_DELETE)
    public ApiResponse<Void> delete(
            @Positive @PathVariable long organizationId,
            @Valid @RequestBody DeleteOrganizationRequest request
    ) {
        service.delete(organizationId, request);
        return ApiResponse.success(null);
    }
}
