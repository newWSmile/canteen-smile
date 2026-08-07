package com.canteen.smile.modules.account.controller;

import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.account.service.AccountActivationService;
import com.canteen.smile.modules.account.service.AccountPasswordResetService;
import com.canteen.smile.modules.account.vo.AccountActivationContextVO;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Auth 通过 HMAC 调用的租户账号激活内部接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class InternalAccountActivationController {

    /** 租户账号激活服务。 */
    private final AccountActivationService service;

    /** 租户账号密码恢复状态服务。 */
    private final AccountPasswordResetService passwordResetService;

    /** @param accountId 账号 ID @return 激活页上下文 */
    @GetMapping(IamApiPaths.TENANT_ACCOUNT_ACTIVATION_CONTEXT)
    public ApiResponse<AccountActivationContextVO> context(@Positive @PathVariable long accountId) {
        return ApiResponse.success(service.context(accountId));
    }

    /** @param accountId 账号 ID @return 激活后的上下文 */
    @PostMapping(IamApiPaths.TENANT_ACCOUNT_ACTIVATE)
    public ApiResponse<AccountActivationContextVO> activate(@Positive @PathVariable long accountId) {
        return ApiResponse.success(service.activate(accountId));
    }

    /** @param accountId 账号 ID @return 完成密码恢复后的上下文 */
    @PostMapping(IamApiPaths.TENANT_ACCOUNT_COMPLETE_PASSWORD_RESET)
    public ApiResponse<AccountActivationContextVO> completePasswordReset(
            @Positive @PathVariable long accountId
    ) {
        return ApiResponse.success(passwordResetService.complete(accountId));
    }
}
