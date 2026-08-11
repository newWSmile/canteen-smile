package com.canteen.smile.modules.sms.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.config.SmsProperties;
import com.canteen.smile.modules.sms.client.SmsClient;
import com.canteen.smile.modules.sms.client.SmsClientConfiguration;
import com.canteen.smile.modules.sms.client.SmsSendCommand;
import com.canteen.smile.modules.sms.dto.SmsChallengeCreateRequest;
import com.canteen.smile.modules.sms.entity.SmsChallengeEntity;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.model.SmsRuntimePolicy;
import com.canteen.smile.modules.sms.vo.SmsChallengeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 创建短信验证码挑战、调用短信策略，并提供绑定用途的一次性校验能力。 */
@Service
@RequiredArgsConstructor
public class SmsChallengeService {

    /** 验证码无效、过期或已使用错误码。 */
    private static final String INVALID_CHALLENGE_CODE = "AUTH_1004";

    /** 短信发送暂时不可用错误码。 */
    private static final String SMS_UNAVAILABLE_CODE = "AUTH_1013";

    /** 短信挑战持久化事务服务。 */
    private final SmsChallengePersistenceService smsChallengePersistenceService;

    /** 验证码安全生成和摘要服务。 */
    private final SmsChallengeCodeService smsChallengeCodeService;

    /** 短信多维频率限制服务。 */
    private final SmsChallengeRateLimitService smsChallengeRateLimitService;

    /** 手机号摘要和脱敏服务。 */
    private final MobileProtectionService mobileProtectionService;

    /** 统一短信策略分发服务。 */
    private final SmsDispatchService smsDispatchService;

    /** 短信策略路由。 */
    private final SmsClientRouter smsClientRouter;

    /** 短信挑战配置。 */
    private final SmsProperties smsProperties;

    /** 数据库当前生效的短信运行策略。 */
    private final SmsRuntimePolicyService smsRuntimePolicyService;

    /**
     * 创建短信挑战并正式调用当前短信策略。
     *
     * @param request 挑战用途、手机号和设备标识
     * @param clientIp 服务端取得的来源 IP
     * @return 不泄露绑定状态的挑战摘要
     */
    public SmsChallengeVO create(SmsChallengeCreateRequest request, String clientIp) {
        SmsRuntimePolicy policy = smsRuntimePolicyService.current();
        /** 手机号安全摘要和脱敏投影。 */
        MobileProtectionService.ProtectedMobile protectedMobile =
                mobileProtectionService.protect(request.getMobile());
        smsChallengeRateLimitService.acquire(
                protectedMobile.hash(), clientIp, request.getDeviceId(), request.getPurpose(), policy
        );

        /** 外部不可预测的挑战标识。 */
        String challengeId = UUID.randomUUID().toString();
        /** 当前调用栈内短暂存在的六位验证码。 */
        String code = smsChallengeCodeService.generate();
        /** 挑战发送时间。 */
        OffsetDateTime sentTime = OffsetDateTime.now(ZoneId.of("Asia/Shanghai"));
        /** 挑战绝对失效时间。 */
        OffsetDateTime expiresAt = sentTime.plusSeconds(policy.challengeTtlSeconds());
        /** 当前配置选择的短信策略。 */
        SmsClient smsClient = smsClientRouter.resolve(smsProperties.getProviderCode());
        /** 当前用途对应的受管供应商和模板配置引用。 */
        SmsClientConfiguration clientConfiguration = smsClient.configurationFor(request.getPurpose());
        /** 新短信挑战实体。 */
        SmsChallengeEntity entity = new SmsChallengeEntity();
        entity.setChallengeId(challengeId);
        entity.setPurpose(request.getPurpose().name());
        entity.setMobileHash(protectedMobile.hash());
        entity.setCodeHash(smsChallengeCodeService.hash(
                challengeId,
                request.getPurpose(),
                protectedMobile.hash(),
                code
        ));
        entity.setProviderConfigId(clientConfiguration.providerConfigId());
        entity.setTemplateConfigId(clientConfiguration.templateConfigId());
        entity.setSentTime(sentTime);
        entity.setExpiresAt(expiresAt);
        smsChallengePersistenceService.create(entity);

        /** 短信发送请求的幂等唯一标识。 */
        String requestId = UUID.randomUUID().toString();
        /** 正文展示的有效分钟数。 */
        long validMinutes = Math.max(1L, (policy.challengeTtlSeconds() + 59L) / 60L);
        /** 当前本地策略可直接使用、真实策略可映射模板参数的发送命令。 */
        SmsSendCommand command = new SmsSendCommand(
                requestId,
                challengeId,
                request.getPurpose(),
                request.getMobile().trim(),
                clientConfiguration.templateCode(),
                "您的验证码是 " + code + "，" + validMinutes + " 分钟内有效，请勿向任何人泄露。",
                Map.of("code", code, "validMinutes", Long.toString(validMinutes)),
                Set.of(code)
        );
        try {
            smsDispatchService.dispatch(command);
        } catch (SmsDispatchException exception) {
            smsChallengePersistenceService.invalidate(challengeId);
            throw new BusinessException(SMS_UNAVAILABLE_CODE, "短信发送暂时不可用，请稍后重试", 503);
        }
        return new SmsChallengeVO(
                challengeId,
                protectedMobile.masked(),
                expiresAt,
                sentTime.plusSeconds(policy.resendIntervalSeconds())
        );
    }

    /**
     * 校验验证码并原子消费挑战；成功结果只能由明确的目的流程继续使用。
     *
     * @param challengeId 挑战标识
     * @param code 用户提交的六位验证码
     * @param expectedPurpose 调用方要求的唯一用途
     * @return 已验证手机号的安全上下文
     */
    public SmsChallengeVerificationResult verifyAndConsume(
            String challengeId,
            String code,
            SmsPurpose expectedPurpose
    ) {
        SmsRuntimePolicy policy = smsRuntimePolicyService.current();
        validateVerificationInput(challengeId, expectedPurpose);
        /** 当前数据库挑战快照。 */
        SmsChallengeEntity entity = smsChallengePersistenceService.find(challengeId);
        if (entity == null || !"PENDING".equals(entity.getStatus())) {
            throw invalidChallenge();
        }
        /** 数据库中受约束的挑战用途。 */
        SmsPurpose actualPurpose;
        try {
            actualPurpose = SmsPurpose.valueOf(entity.getPurpose());
        } catch (IllegalArgumentException exception) {
            throw invalidChallenge();
        }
        if (actualPurpose != expectedPurpose) {
            throw invalidChallenge();
        }
        /** 当前验证码校验时间。 */
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Shanghai"));
        if (entity.getExpiresAt() == null || !entity.getExpiresAt().isAfter(now)) {
            smsChallengePersistenceService.markExpired(challengeId);
            throw invalidChallenge();
        }
        if (entity.getAttempts() == null
                || entity.getAttempts() >= policy.maxVerificationAttempts()) {
            throw invalidChallenge();
        }
        boolean matched = smsChallengeCodeService.matches(
                entity.getCodeHash(),
                challengeId,
                actualPurpose,
                entity.getMobileHash(),
                code
        );
        if (!matched) {
            smsChallengePersistenceService.recordFailure(
                    challengeId,
                    policy.maxVerificationAttempts()
            );
            throw invalidChallenge();
        }
        boolean consumed = smsChallengePersistenceService.consume(
                challengeId,
                policy.maxVerificationAttempts()
        );
        if (!consumed) {
            throw invalidChallenge();
        }
        return new SmsChallengeVerificationResult(challengeId, actualPurpose, entity.getMobileHash());
    }

    /** 校验内部用途流程传入的挑战标识和用途。 */
    private void validateVerificationInput(String challengeId, SmsPurpose expectedPurpose) {
        if (challengeId == null || challengeId.isBlank() || expectedPurpose == null) {
            throw invalidChallenge();
        }
    }

    /** @return 不泄露挑战存在性和失败次数的统一异常 */
    private BusinessException invalidChallenge() {
        return new BusinessException(INVALID_CHALLENGE_CODE, "验证码无效、已过期或已使用", 400);
    }
}
