package com.canteen.smile.internal.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.internal.dto.SmsRateLimitPolicyUpdateInternalRequest;
import com.canteen.smile.internal.dto.SmsRuntimePolicyInternalResponse;
import com.canteen.smile.internal.dto.SmsSecurityPolicyUpdateInternalRequest;
import com.canteen.smile.modules.sms.model.SmsRuntimePolicy;
import com.canteen.smile.modules.sms.service.SmsRuntimePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 仅供 IAM 经内部网络和 HMAC 管理全局短信策略的接口。 */
@RestController
@RequiredArgsConstructor
public class SmsRuntimePolicyInternalController {

    /** 短信运行策略事务服务。 */
    private final SmsRuntimePolicyService service;

    /** @return 当前短信运行策略 */
    @GetMapping(AuthApiPaths.INTERNAL_SMS_RUNTIME_POLICY)
    public ApiResponse<SmsRuntimePolicyInternalResponse> current() {
        return ApiResponse.success(response(service.current()));
    }

    /** @param request 限流设置及管理员再认证票据 @return 更新后策略 */
    @PutMapping(AuthApiPaths.INTERNAL_SMS_RATE_LIMIT_POLICY)
    public ApiResponse<SmsRuntimePolicyInternalResponse> updateRateLimits(
            @Valid @RequestBody SmsRateLimitPolicyUpdateInternalRequest request
    ) {
        return ApiResponse.success(response(service.updateRateLimits(request)));
    }

    /** @param request 安全设置及管理员再认证票据 @return 更新后策略 */
    @PutMapping(AuthApiPaths.INTERNAL_SMS_SECURITY_POLICY)
    public ApiResponse<SmsRuntimePolicyInternalResponse> updateSecurity(
            @Valid @RequestBody SmsSecurityPolicyUpdateInternalRequest request
    ) {
        return ApiResponse.success(response(service.updateSecurity(request)));
    }

    /** @param policy 内部运行策略 @return 稳定版本化响应 */
    private SmsRuntimePolicyInternalResponse response(SmsRuntimePolicy policy) {
        return new SmsRuntimePolicyInternalResponse(
                policy.challengeTtlSeconds(), policy.resendIntervalSeconds(),
                policy.maxVerificationAttempts(), policy.mobileHourlyLimit(),
                policy.mobileDailyLimit(), policy.ipHourlyLimit(), policy.ipDailyLimit(),
                policy.deviceHourlyLimit(), policy.deviceDailyLimit(),
                policy.plaintextCodeRetentionEnabled(), policy.updatedTime(), policy.version()
        );
    }
}
