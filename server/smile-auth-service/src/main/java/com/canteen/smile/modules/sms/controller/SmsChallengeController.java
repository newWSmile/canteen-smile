package com.canteen.smile.modules.sms.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.sms.dto.SmsChallengeCreateRequest;
import com.canteen.smile.modules.sms.service.SmsChallengeService;
import com.canteen.smile.modules.sms.vo.SmsChallengeVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 匿名短信验证码挑战创建接口。 */
@Validated
@RestController
@RequiredArgsConstructor
public class SmsChallengeController {

    /** 短信挑战创建和一次性校验服务。 */
    private final SmsChallengeService smsChallengeService;

    /**
     * 创建短信挑战并通过当前策略发送验证码。
     *
     * @param request 手机号、用途和设备限流标识
     * @param servletRequest 当前 HTTP 请求
     * @return 脱敏挑战摘要
     */
    @PostMapping(AuthApiPaths.SMS_CHALLENGES)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SmsChallengeVO> createChallenge(
            @Valid @RequestBody SmsChallengeCreateRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(smsChallengeService.create(request, servletRequest.getRemoteAddr()));
    }
}
