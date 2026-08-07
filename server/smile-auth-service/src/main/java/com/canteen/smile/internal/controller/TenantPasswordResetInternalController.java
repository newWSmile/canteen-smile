package com.canteen.smile.internal.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.internal.dto.TenantPasswordResetTicketRequest;
import com.canteen.smile.internal.dto.TenantPasswordResetTicketResponse;
import com.canteen.smile.modules.auth.service.TenantPasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** IAM 通过 HMAC 调用的租户账号密码恢复票据接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class TenantPasswordResetInternalController {

    /** 租户账号密码恢复服务。 */
    private final TenantPasswordResetService resetService;

    /**
     * 消费平台再认证票据并签发 30 分钟一次性恢复票据。
     *
     * @param accountId 租户账号 ID
     * @param request 已校验的平台敏感操作上下文
     * @return 只在当前响应展示的恢复票据
     */
    @PostMapping(AuthApiPaths.INTERNAL_TENANT_ACCOUNT_PASSWORD_RESET_TICKETS)
    public ApiResponse<TenantPasswordResetTicketResponse> issue(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String accountId,
            @Valid @RequestBody TenantPasswordResetTicketRequest request
    ) {
        return ApiResponse.success(resetService.issue(Long.parseLong(accountId), request));
    }
}
