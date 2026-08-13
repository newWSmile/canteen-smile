package com.canteen.smile.modules.organization.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.organization.dto.TransferOrganizationOwnerRequest;
import com.canteen.smile.modules.organization.service.OrganizationOwnerService;
import com.canteen.smile.modules.organization.vo.OrganizationOwnerVO;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前机构所有权接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.TENANT_ORGANIZATION_OWNER)
public class OrganizationOwnerController {
    /** 所有权服务。 */
    private final OrganizationOwnerService service;

    /** @return 当前机构所有者 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_OWNER_VIEW)
    public ApiResponse<OrganizationOwnerVO> current() {
        return ApiResponse.success(service.current());
    }

    /** @param request 转让请求 @return 新所有者 */
    @PostMapping("/actions/transfer")
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_OWNER_TRANSFER)
    public ApiResponse<OrganizationOwnerVO> transfer(@Valid @RequestBody TransferOrganizationOwnerRequest request) {
        return ApiResponse.success(service.transfer(request));
    }
}
