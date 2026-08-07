package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.auth.dto.CompleteActivationRequest;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.service.PasswordEnvelopeService;
import com.canteen.smile.modules.auth.service.TenantAccountActivationService;
import com.canteen.smile.modules.auth.vo.ActivationCompleteVO;
import com.canteen.smile.modules.auth.vo.ActivationContextVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 匿名账号激活上下文和完成接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class ActivationController {

    /** 租户账号激活服务。 */
    private final TenantAccountActivationService activationService;

    /** 密码信封解密服务。 */
    private final PasswordEnvelopeService passwordEnvelopeService;

    /** @param ticket 一次性激活票据 @return 脱敏账号上下文 */
    @GetMapping(AuthApiPaths.ACTIVATION_CONTEXT)
    public ApiResponse<ActivationContextVO> context(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9_-]{43,128}$") String ticket
    ) {
        return ApiResponse.success(activationService.context(ticket));
    }

    /**
     * 使用一次性票据设置初始密码并激活账号。
     *
     * @param ticket 一次性激活票据
     * @param request 加密密码请求
     * @return 激活完成结果
     */
    @PostMapping(AuthApiPaths.ACTIVATION_COMPLETE)
    public ApiResponse<ActivationCompleteVO> complete(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9_-]{43,128}$") String ticket,
            @Valid @RequestBody CompleteActivationRequest request
    ) {
        String password = passwordEnvelopeService.decrypt(
                request.passwordEnvelope(),
                PasswordEnvelopePurpose.TENANT_ACCOUNT_ACTIVATION
        );
        return ApiResponse.success(activationService.complete(ticket, password));
    }
}
