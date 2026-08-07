package com.canteen.smile.modules.organization.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.organization.dto.ReplaceOrganizationTypeRelationsRequest;
import com.canteen.smile.modules.organization.service.TenantOrganizationTypeService;
import com.canteen.smile.modules.organization.vo.OrganizationTypeRelationVO;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 租户机构类型允许关系接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.TENANT_ORGANIZATION_TYPE_RELATIONS)
public class TenantOrganizationTypeRelationController {

    /** 机构类型领域服务。 */
    private final TenantOrganizationTypeService service;

    /** @return 当前租户有效允许关系 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_TYPE_VIEW)
    public ApiResponse<List<OrganizationTypeRelationVO>> relations() {
        return ApiResponse.success(service.relations());
    }

    /** @param request 完整允许关系集合 @return 保存后的关系 */
    @PutMapping
    @SaCheckPermission(IamPermissionCodes.IAM_ORG_TYPE_MANAGE)
    public ApiResponse<List<OrganizationTypeRelationVO>> replace(
            @Valid @RequestBody ReplaceOrganizationTypeRelationsRequest request
    ) {
        return ApiResponse.success(service.replaceRelations(request));
    }
}
