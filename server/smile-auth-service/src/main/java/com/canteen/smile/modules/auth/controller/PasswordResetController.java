package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.auth.dto.CompletePasswordResetRequest;
import com.canteen.smile.modules.auth.dto.SmsPasswordResetAccountSelectionRequest;
import com.canteen.smile.modules.auth.dto.SmsPasswordResetVerificationRequest;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.service.PasswordEnvelopeService;
import com.canteen.smile.modules.auth.service.TenantPasswordResetService;
import com.canteen.smile.modules.auth.service.TenantPasswordRecoveryService;
import com.canteen.smile.modules.auth.vo.PasswordResetCompleteVO;
import com.canteen.smile.modules.auth.vo.PasswordResetContextVO;
import com.canteen.smile.modules.auth.vo.SmsPasswordResetResultVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 匿名一次性密码恢复链接上下文和完成接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class PasswordResetController {

    /** 租户账号密码恢复服务。 */
    private final TenantPasswordResetService resetService;

    /** 密码信封解密服务。 */
    private final PasswordEnvelopeService passwordEnvelopeService;

    /** 手机号验证码自助找回密码服务。 */
    private final TenantPasswordRecoveryService passwordRecoveryService;

    /**
     * 消费 PASSWORD_RESET 用途验证码并开始自助找回密码。
     *
     * @param request 短信验证码校验请求
     * @return 设置新密码或账号选择结果
     */
    @PostMapping(AuthApiPaths.PASSWORD_RESET_SMS_VERIFICATION)
    public ApiResponse<SmsPasswordResetResultVO> verifySms(
            @Valid @RequestBody SmsPasswordResetVerificationRequest request
    ) {
        return ApiResponse.success(passwordRecoveryService.verify(request));
    }

    /**
     * 手机号绑定多个账号时选择本次需要找回密码的具体账号。
     *
     * @param request 账号选择请求
     * @return 设置新密码步骤及一次性重置票据
     */
    @PostMapping(AuthApiPaths.PASSWORD_RESET_SMS_ACCOUNT_SELECTION)
    public ApiResponse<SmsPasswordResetResultVO> selectSmsAccount(
            @Valid @RequestBody SmsPasswordResetAccountSelectionRequest request
    ) {
        return ApiResponse.success(passwordRecoveryService.selectAccount(request));
    }

    /** @param ticket 一次性恢复票据 @return 脱敏账号上下文 */
    @GetMapping(AuthApiPaths.PASSWORD_RESET_CONTEXT)
    public ApiResponse<PasswordResetContextVO> context(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9_-]{43,128}$") String ticket
    ) {
        return ApiResponse.success(resetService.context(ticket));
    }

    /**
     * 使用一次性票据设置新密码并恢复账号。
     *
     * @param ticket 一次性恢复票据
     * @param request 加密的新密码
     * @return 密码恢复完成结果
     */
    @PostMapping(AuthApiPaths.PASSWORD_RESET_COMPLETE)
    public ApiResponse<PasswordResetCompleteVO> complete(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9_-]{43,128}$") String ticket,
            @Valid @RequestBody CompletePasswordResetRequest request
    ) {
        /** 仅在当前调用栈短暂存在的新密码明文。 */
        String password = passwordEnvelopeService.decrypt(
                request.passwordEnvelope(),
                PasswordEnvelopePurpose.TENANT_PASSWORD_RESET
        );
        return ApiResponse.success(resetService.complete(ticket, password));
    }
}
