package com.canteen.smile.internal.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.internal.dto.TenantAccountProvisionRequest;
import com.canteen.smile.internal.dto.TenantAccountProvisionResponse;
import com.canteen.smile.internal.dto.TenantActivationTicketInternalResponse;
import com.canteen.smile.modules.auth.service.TenantAccountCredentialService;
import com.canteen.smile.modules.auth.service.TenantAccountActivationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 仅供 IAM 经 HMAC 调用的租户账号凭证初始化接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class TenantAccountProvisionController {

    /** 租户账号凭证初始化服务。 */
    private final TenantAccountCredentialService credentialService;

    /** 租户账号激活票据服务。 */
    private final TenantAccountActivationService activationService;

    /**
     * 幂等创建待激活租户账号凭证容器。
     *
     * @param accountId IAM 账号 ID 字符串
     * @param request 租户和机构上下文
     * @return 统一初始化响应
     */
    @PostMapping(AuthApiPaths.INTERNAL_TENANT_ACCOUNT_PROVISION)
    public ApiResponse<TenantAccountProvisionResponse> provision(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String accountId,
            @Valid @RequestBody TenantAccountProvisionRequest request
    ) {
        return ApiResponse.success(credentialService.provision(Long.parseLong(accountId)));
    }

    /**
     * 废弃此前未使用票据并生成新的 24 小时一次性激活票据。
     *
     * @param accountId IAM 租户账号 ID
     * @return 只在本次响应展示的激活票据
     */
    @PostMapping(AuthApiPaths.INTERNAL_TENANT_ACCOUNT_ACTIVATION_TICKETS)
    public ApiResponse<TenantActivationTicketInternalResponse> issueActivationTicket(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String accountId
    ) {
        return ApiResponse.success(activationService.issue(Long.parseLong(accountId)));
    }
}
