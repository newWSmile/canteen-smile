package com.canteen.smile.modules.tenant.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.tenant.dto.UpdateTenantSecurityPolicyRequest;
import com.canteen.smile.modules.tenant.service.TenantSecurityPolicyService;
import com.canteen.smile.modules.tenant.vo.TenantSecurityPolicyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 租户根机构所有者维护登录、会话、密码和审计保留策略的接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping(IamApiPaths.TENANT_SECURITY_POLICY)
public class TenantSecurityPolicyController {

    /** 租户安全策略应用服务。 */
    private final TenantSecurityPolicyService service;

    /** @return 当前租户安全策略。 */
    @GetMapping
    @SaCheckPermission(IamPermissionCodes.IAM_TENANT_SECURITY_VIEW)
    public ApiResponse<TenantSecurityPolicyVO> current() {
        return ApiResponse.success(service.current());
    }

    /** @param request 修改命令 @return 修改后的安全策略。 */
    @PutMapping
    @SaCheckPermission(IamPermissionCodes.IAM_TENANT_SECURITY_MANAGE)
    public ApiResponse<TenantSecurityPolicyVO> update(
            @Valid @RequestBody UpdateTenantSecurityPolicyRequest request
    ) {
        return ApiResponse.success(service.update(request));
    }
}
