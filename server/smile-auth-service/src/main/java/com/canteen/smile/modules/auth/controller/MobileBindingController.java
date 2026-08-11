package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.auth.dto.MobileBindingChallengeRequest;
import com.canteen.smile.modules.auth.dto.MobileBindingConfirmRequest;
import com.canteen.smile.modules.auth.service.MobileBindingService;
import com.canteen.smile.modules.auth.vo.MobileBindingStatusVO;
import com.canteen.smile.modules.sms.vo.SmsChallengeVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 当前已登录租户账号首次绑定手机号接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class MobileBindingController {

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
                servletRequest.getRemoteAddr()
        ));
    }

    /** @param request 手机号、挑战和验证码 @return 已验证绑定状态 */
    @PostMapping(AuthApiPaths.MOBILE_BINDING_CONFIRM)
    public ApiResponse<MobileBindingStatusVO> confirm(
            @Valid @RequestBody MobileBindingConfirmRequest request
    ) {
        return ApiResponse.success(mobileBindingService.confirm(request));
    }
}
