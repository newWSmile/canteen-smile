package com.canteen.smile.modules.sms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.api.IamApiPaths;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.sms.dto.SmsRateLimitSettingsUpdateRequest;
import com.canteen.smile.modules.sms.dto.SmsSecuritySettingsUpdateRequest;
import com.canteen.smile.modules.sms.service.PlatformSmsPolicyService;
import com.canteen.smile.modules.sms.vo.SmsRuntimePolicyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 平台超级管理员短信限流和安全设置接口。 */
@RestController
@RequiredArgsConstructor
public class PlatformSmsPolicyController {

    /** 平台短信设置编排服务。 */
    private final PlatformSmsPolicyService service;

    /** @return 当前短信限流设置 */
    @GetMapping(IamApiPaths.PLATFORM_SMS_SETTINGS)
    @SaCheckPermission(IamPermissionCodes.PLATFORM_SMS_SETTINGS_VIEW)
    public ApiResponse<SmsRuntimePolicyVO> rateLimitSettings() {
        return ApiResponse.success(service.current());
    }

    /** @param request 限流设置及敏感操作认证信息 @return 更新后策略 */
    @PutMapping(IamApiPaths.PLATFORM_SMS_SETTINGS)
    @SaCheckPermission(IamPermissionCodes.PLATFORM_SMS_SETTINGS_UPDATE)
    public ApiResponse<SmsRuntimePolicyVO> updateRateLimitSettings(
            @Valid @RequestBody SmsRateLimitSettingsUpdateRequest request
    ) {
        return ApiResponse.success(service.updateRateLimits(request));
    }

    /** @return 当前短信安全设置 */
    @GetMapping(IamApiPaths.PLATFORM_SMS_SECURITY)
    @SaCheckPermission(IamPermissionCodes.PLATFORM_SMS_SECURITY_VIEW)
    public ApiResponse<SmsRuntimePolicyVO> securitySettings() {
        return ApiResponse.success(service.current());
    }

    /** @param request 安全开关及敏感操作认证信息 @return 更新后策略 */
    @PutMapping(IamApiPaths.PLATFORM_SMS_SECURITY)
    @SaCheckPermission(IamPermissionCodes.PLATFORM_SMS_SECURITY_UPDATE)
    public ApiResponse<SmsRuntimePolicyVO> updateSecuritySettings(
            @Valid @RequestBody SmsSecuritySettingsUpdateRequest request
    ) {
        return ApiResponse.success(service.updateSecurity(request));
    }
}
