package com.canteen.smile.modules.sms.service;

import com.canteen.smile.internal.client.AuthSmsPolicyClient;
import com.canteen.smile.internal.client.dto.SmsRateLimitPolicyUpdateInternalRequest;
import com.canteen.smile.internal.client.dto.SmsRuntimePolicyInternalResponse;
import com.canteen.smile.internal.client.dto.SmsSecurityPolicyUpdateInternalRequest;
import com.canteen.smile.modules.audit.service.IamAuditLogService;
import com.canteen.smile.modules.platform.service.PlatformActorService;
import com.canteen.smile.modules.sms.dto.SmsRateLimitSettingsUpdateRequest;
import com.canteen.smile.modules.sms.dto.SmsSecuritySettingsUpdateRequest;
import com.canteen.smile.modules.sms.vo.SmsRuntimePolicyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 平台短信设置编排服务，IAM 负责权限、操作人和审计，数据仍归 Auth。 */
@Service
@RequiredArgsConstructor
public class PlatformSmsPolicyService {

    /** Auth 短信策略 Client。 */
    private final AuthSmsPolicyClient client;
    /** 当前平台身份解析服务。 */
    private final PlatformActorService actorService;
    /** IAM 管理审计服务。 */
    private final IamAuditLogService auditLogService;

    /** @return 当前短信运行策略 */
    public SmsRuntimePolicyVO current() {
        return toVo(client.current());
    }

    /** @param request 限流设置、原因和再认证票据 @return 更新后策略 */
    public SmsRuntimePolicyVO updateRateLimits(SmsRateLimitSettingsUpdateRequest request) {
        long actorId = actorService.currentPlatformIdentityId();
        try {
            SmsRuntimePolicyInternalResponse response = client.updateRateLimits(
                    new SmsRateLimitPolicyUpdateInternalRequest(
                            request.challengeTtlSeconds(), request.resendIntervalSeconds(),
                            request.maxVerificationAttempts(), request.mobileHourlyLimit(),
                            request.mobileDailyLimit(), request.ipHourlyLimit(), request.ipDailyLimit(),
                            request.deviceHourlyLimit(), request.deviceDailyLimit(), request.version(),
                            actorId, request.reauthTicket()
                    )
            );
            audit(actorId, "platform:sms-settings:update", "修改短信限流设置", request.reason(), "SUCCESS");
            return toVo(response);
        } catch (RuntimeException exception) {
            audit(actorId, "platform:sms-settings:update", "修改短信限流设置", request.reason(), "FAILURE");
            throw exception;
        }
    }

    /** @param request 安全开关、原因和再认证票据 @return 更新后策略 */
    public SmsRuntimePolicyVO updateSecurity(SmsSecuritySettingsUpdateRequest request) {
        long actorId = actorService.currentPlatformIdentityId();
        try {
            SmsRuntimePolicyInternalResponse response = client.updateSecurity(
                    new SmsSecurityPolicyUpdateInternalRequest(
                            request.plaintextCodeRetentionEnabled(), request.version(),
                            actorId, request.reauthTicket()
                    )
            );
            audit(actorId, "platform:sms-security:update", "修改短信安全设置", request.reason(), "SUCCESS");
            return toVo(response);
        } catch (RuntimeException exception) {
            audit(actorId, "platform:sms-security:update", "修改短信安全设置", request.reason(), "FAILURE");
            throw exception;
        }
    }

    /** 追加平台敏感短信设置审计。 */
    private void audit(long actorId, String code, String name, String reason, String result) {
        auditLogService.recordPlatformActionWithReason(
                actorId, code, name, "SMS_RUNTIME_POLICY", "GLOBAL", reason.strip(), result
        );
    }

    /** @param response Auth 内部契约 @return 平台稳定响应 */
    private SmsRuntimePolicyVO toVo(SmsRuntimePolicyInternalResponse response) {
        return new SmsRuntimePolicyVO(
                response.challengeTtlSeconds(), response.resendIntervalSeconds(),
                response.maxVerificationAttempts(), response.mobileHourlyLimit(),
                response.mobileDailyLimit(), response.ipHourlyLimit(), response.ipDailyLimit(),
                response.deviceHourlyLimit(), response.deviceDailyLimit(),
                response.plaintextCodeRetentionEnabled(), response.updatedTime(), response.version()
        );
    }
}
