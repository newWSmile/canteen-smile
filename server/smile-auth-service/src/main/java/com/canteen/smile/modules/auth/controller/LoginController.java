package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.auth.dto.PasswordLoginRequest;
import com.canteen.smile.modules.auth.dto.PlatformRecoveryLoginRequest;
import com.canteen.smile.modules.auth.dto.SmsLoginRequest;
import com.canteen.smile.modules.auth.dto.AccountSelectionLoginRequest;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.service.PasswordEnvelopeService;
import com.canteen.smile.modules.auth.service.PlatformPasswordLoginService;
import com.canteen.smile.modules.auth.service.TenantPasswordLoginService;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.service.PlatformRecoveryLoginService;
import com.canteen.smile.modules.auth.service.TenantSmsLoginService;
import com.canteen.smile.modules.auth.service.ClientIpService;
import com.canteen.smile.modules.auth.vo.LoginResultVO;
import com.canteen.smile.modules.auth.vo.SessionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 匿名登录流程接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class LoginController {

    /** 平台密码登录服务。 */
    private final PlatformPasswordLoginService platformPasswordLoginService;

    /** 租户管理端密码登录服务。 */
    private final TenantPasswordLoginService tenantPasswordLoginService;

    /** 平台恢复码二次验证服务。 */
    private final PlatformRecoveryLoginService platformRecoveryLoginService;

    /** 一次性密码信封解密服务。 */
    private final PasswordEnvelopeService passwordEnvelopeService;

    /** 租户手机号验证码登录与账号选择服务。 */
    private final TenantSmsLoginService tenantSmsLoginService;

    /** 网关可信客户端 IP 解析服务。 */
    private final ClientIpService clientIpService;

    /**
     * 校验平台用户名和密码并直接建立设备会话。
     *
     * @param request 密码登录请求
     * @param servletRequest 当前 HTTP 请求
     * @return 已完成认证的登录结果
     */
    @PostMapping(AuthApiPaths.PASSWORD_LOGIN)
    public ApiResponse<LoginResultVO> passwordLogin(
            @Valid @RequestBody PasswordLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        /** 仅在当前认证调用链中短暂存在的密码明文。 */
        boolean tenantAdmin = AuthConstants.TENANT_ADMIN_APP.equals(request.getAppCode());
        String password = passwordEnvelopeService.decrypt(
                request.getPasswordEnvelope(),
                tenantAdmin
                        ? PasswordEnvelopePurpose.TENANT_PASSWORD_LOGIN
                        : PasswordEnvelopePurpose.PLATFORM_PASSWORD_LOGIN
        );
        return ApiResponse.success(tenantAdmin
                ? tenantPasswordLoginService.login(request, password, clientIpService.resolve(servletRequest))
                : platformPasswordLoginService.login(request, password, clientIpService.resolve(servletRequest)));
    }

    /**
     * 使用 LOGIN 用途短信验证码登录，手机号绑定多个账号时返回选择候选。
     *
     * @param request 短信验证码登录请求
     * @param servletRequest 当前 HTTP 请求
     * @return 已认证会话或账号选择结果
     */
    @PostMapping(AuthApiPaths.SMS_LOGIN)
    public ApiResponse<LoginResultVO> smsLogin(
            @Valid @RequestBody SmsLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(tenantSmsLoginService.login(request, clientIpService.resolve(servletRequest)));
    }

    /**
     * 使用短期一次性票据选择手机号绑定的具体账号并完成登录。
     *
     * @param request 账号选择请求
     * @param servletRequest 当前 HTTP 请求
     * @return 已建立的设备会话
     */
    @PostMapping(AuthApiPaths.ACCOUNT_SELECTION_LOGIN)
    public ApiResponse<LoginResultVO> accountSelectionLogin(
            @Valid @RequestBody AccountSelectionLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(tenantSmsLoginService.selectAccount(
                request, clientIpService.resolve(servletRequest)
        ));
    }

    /**
     * 使用平台一次性恢复码完成二次验证。
     *
     * @param request 恢复码二次验证请求
     * @param servletRequest 当前 HTTP 请求
     * @return 已建立的设备会话
     */
    @PostMapping(AuthApiPaths.PLATFORM_RECOVERY_LOGIN)
    public ApiResponse<SessionVO> platformRecoveryLogin(
            @Valid @RequestBody PlatformRecoveryLoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(platformRecoveryLoginService.login(
                request, clientIpService.resolve(servletRequest)
        ));
    }
}
