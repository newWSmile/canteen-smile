package com.canteen.smile.modules.account.controller;

import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.account.service.TenantManagementContextService;
import com.canteen.smile.modules.account.vo.TenantManagementContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 租户管理端当前身份与权限启动接口。 */
@RestController
@RequiredArgsConstructor
public class TenantManagementContextController {

    /** 租户管理端启动上下文服务。 */
    private final TenantManagementContextService service;

    /** @return 当前登录租户账号的真实管理上下文 */
    @GetMapping(IamApiPaths.TENANT_CONTEXT)
    public ApiResponse<TenantManagementContextVO> current() {
        return ApiResponse.success(service.current());
    }
}
