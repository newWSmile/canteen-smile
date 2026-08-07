package com.canteen.smile.modules.organization.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.organization.dto.PublishOrgTypeTemplateRequest;
import com.canteen.smile.modules.organization.service.OrgTypeTemplateService;
import com.canteen.smile.modules.organization.vo.OrgTypeTemplateVO;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 平台机构类型模板发布接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.PLATFORM_ORG_TYPE_TEMPLATES)
public class PlatformOrgTypeTemplateController {

    /** 模板领域服务。 */
    private final OrgTypeTemplateService service;

    /** @return 所有已发布模板版本 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.PLATFORM_ORG_TEMPLATE_MANAGE)
    public ApiResponse<List<OrgTypeTemplateVO>> listPublished() {
        return ApiResponse.success(service.listPublished());
    }

    /** @param request 完整模板 @return 新发布版本 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SaCheckPermission(IamPermissionCodes.PLATFORM_ORG_TEMPLATE_MANAGE)
    public ApiResponse<OrgTypeTemplateVO> publish(@Valid @RequestBody PublishOrgTypeTemplateRequest request) {
        return ApiResponse.success(service.publish(request));
    }
}
