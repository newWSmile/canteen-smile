package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.auth.dto.CurrentMobileChallengeRequest;
import com.canteen.smile.modules.auth.dto.CurrentMobileVerificationRequest;
import com.canteen.smile.modules.auth.dto.MobileBindingChallengeRequest;
import com.canteen.smile.modules.auth.dto.MobileBindingConfirmRequest;
import com.canteen.smile.modules.auth.dto.MobileChangeChallengeRequest;
import com.canteen.smile.modules.auth.dto.MobileChangeConfirmRequest;
import com.canteen.smile.modules.auth.dto.MobileUnbindConfirmRequest;
import com.canteen.smile.modules.auth.service.MobileBindingService;
import com.canteen.smile.modules.auth.vo.MobileBindingStatusVO;
import com.canteen.smile.modules.auth.vo.ReauthTicketVO;
import com.canteen.smile.modules.sms.vo.SmsChallengeVO;
import jakarta.servlet.http.HttpServletRequest;
import com.canteen.smile.modules.auth.service.ClientIpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 当前已登录租户账号手机号绑定、换绑与解绑安全接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class MobileBindingController {

    /** 网关可信客户端 IP 解析服务。 */
    private final ClientIpService clientIpService;

    /** 手机号绑定业务编排服务。 */
    private final MobileBindingService mobileBindingService;

    /** @return 当前账号不泄露完整手机号的绑定状态 */
    @GetMapping(AuthApiPaths.MOBILE_BINDING)
    public ApiResponse<MobileBindingStatusVO> current() {
        return ApiResponse.success(mobileBindingService.current());
    }

    /**
     * 创建当前账号首次绑定手机号验证码挑战。
     *
     * @param request 手机号和设备限流上下文
     * @param servletRequest 当前 HTTP 请求
     * @return 脱敏挑战摘要
     */
    @PostMapping(AuthApiPaths.MOBILE_BINDING_CHALLENGES)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SmsChallengeVO> createChallenge(
            @Valid @RequestBody MobileBindingChallengeRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(mobileBindingService.createChallenge(
                request,
                clientIpService.resolve(servletRequest)
        ));
    }

    /** @param request 手机号、挑战和验证码 @return 已验证绑定状态 */
    @PostMapping(AuthApiPaths.MOBILE_BINDING_CONFIRM)
    public ApiResponse<MobileBindingStatusVO> confirm(
            @Valid @RequestBody MobileBindingConfirmRequest request
    ) {
        return ApiResponse.success(mobileBindingService.confirm(request));
    }

    /**
     * 向当前已验证手机号发送换绑或解绑验证码。
     *
     * @param request 当前设备限流上下文
     * @param servletRequest 当前 HTTP 请求
     * @return 当前手机号脱敏挑战摘要
     */
    @PostMapping(AuthApiPaths.MOBILE_BINDING_CURRENT_CHALLENGES)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SmsChallengeVO> createCurrentMobileChallenge(
            @Valid @RequestBody CurrentMobileChallengeRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(mobileBindingService.createCurrentMobileChallenge(
                request, clientIpService.resolve(servletRequest)
        ));
    }

    /** @param request 当前手机号验证码与唯一动作 @return 五分钟单用途再认证票据 */
    @PostMapping(AuthApiPaths.MOBILE_BINDING_CURRENT_VERIFICATION)
    public ApiResponse<ReauthTicketVO> verifyCurrentMobile(
            @Valid @RequestBody CurrentMobileVerificationRequest request
    ) {
        return ApiResponse.success(mobileBindingService.verifyCurrentMobile(request));
    }

    /**
     * 向与当前号码不同的新手机号发送换绑验证码。
     *
     * @param request 新手机号与设备限流上下文
     * @param servletRequest 当前 HTTP 请求
     * @return 新手机号脱敏挑战摘要
     */
    @PostMapping(AuthApiPaths.MOBILE_BINDING_CHANGE_CHALLENGES)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SmsChallengeVO> createChangeChallenge(
            @Valid @RequestBody MobileChangeChallengeRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(mobileBindingService.createChangeChallenge(
                request, clientIpService.resolve(servletRequest)
        ));
    }

    /** @param request 再认证票据与新手机号验证码 @return 新手机号绑定状态 */
    @PostMapping(AuthApiPaths.MOBILE_BINDING_CHANGE_CONFIRM)
    public ApiResponse<MobileBindingStatusVO> change(
            @Valid @RequestBody MobileChangeConfirmRequest request
    ) {
        return ApiResponse.success(mobileBindingService.change(request));
    }

    /** @param request 仅允许解绑的一次性再认证票据 @return 未绑定状态 */
    @PostMapping(AuthApiPaths.MOBILE_BINDING_UNBIND_CONFIRM)
    public ApiResponse<MobileBindingStatusVO> unbind(
            @Valid @RequestBody MobileUnbindConfirmRequest request
    ) {
        return ApiResponse.success(mobileBindingService.unbind(request));
    }
}
