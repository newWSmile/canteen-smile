package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.dto.MobileBindingChallengeRequest;
import com.canteen.smile.modules.auth.dto.MobileBindingConfirmRequest;
import com.canteen.smile.modules.auth.entity.MobileBindingEntity;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.vo.MobileBindingStatusVO;
import com.canteen.smile.modules.sms.dto.SmsChallengeCreateRequest;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.service.MobileProtectionService;
import com.canteen.smile.modules.sms.service.SmsChallengeService;
import com.canteen.smile.modules.sms.vo.SmsChallengeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;

/** 当前租户账号首次绑定已验证手机号的业务编排服务。 */
@Service
@RequiredArgsConstructor
public class MobileBindingService {

    /** 当前账号已经绑定手机号的错误码。 */
    private static final String ALREADY_BOUND_CODE = "AUTH_1301";

    /** 手机号与挑战不匹配的稳定错误码。 */
    private static final String CHALLENGE_MISMATCH_CODE = "AUTH_1004";

    /** 手机号安全密钥配置不可用的错误码。 */
    private static final String MOBILE_SECURITY_UNAVAILABLE_CODE = "AUTH_1014";

    /** 当前可信登录主体解析服务。 */
    private final CurrentAuthSubjectService currentAuthSubjectService;

    /** 手机号绑定事务服务。 */
    private final MobileBindingPersistenceService persistenceService;

    /** 短信挑战创建和消费服务。 */
    private final SmsChallengeService smsChallengeService;

    /** 手机号归一化、摘要与脱敏服务。 */
    private final MobileProtectionService mobileProtectionService;

    /** 手机号可轮换认证加密服务。 */
    private final MobileCipherService mobileCipherService;

    /** @return 当前账号手机号绑定安全状态 */
    public MobileBindingStatusVO current() {
        CurrentAuthSubjectService.CurrentTenantSubject subject = currentAuthSubjectService.currentTenant();
        MobileBindingEntity entity = persistenceService.findVerified(
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                subject.accountId()
        );
        return entity == null
                ? MobileBindingStatusVO.unbound()
                : new MobileBindingStatusVO(true, entity.getMaskedMobile(), entity.getVerifiedTime());
    }

    /**
     * 为当前账号创建首次绑定手机号验证码挑战。
     *
     * @param request 手机号与设备限流上下文
     * @param clientIp 服务端取得的来源 IP
     * @return 脱敏短信挑战摘要
     */
    public SmsChallengeVO createChallenge(MobileBindingChallengeRequest request, String clientIp) {
        CurrentAuthSubjectService.CurrentTenantSubject subject = currentAuthSubjectService.currentTenant();
        rejectAlreadyBound(subject.accountId());
        ensureCipherConfigured();
        SmsChallengeCreateRequest challengeRequest = new SmsChallengeCreateRequest();
        challengeRequest.setPurpose(SmsPurpose.MOBILE_BIND);
        challengeRequest.setMobile(request.getMobile());
        challengeRequest.setDeviceId(request.getDeviceId());
        challengeRequest.setCaptchaTicket(request.getCaptchaTicket());
        return smsChallengeService.create(challengeRequest, clientIp);
    }

    /**
     * 原子消费验证码并保存当前账号首次手机号绑定。
     *
     * @param request 手机号、挑战和验证码
     * @return 已绑定的脱敏安全状态
     */
    @Transactional
    public MobileBindingStatusVO confirm(MobileBindingConfirmRequest request) {
        CurrentAuthSubjectService.CurrentTenantSubject subject = currentAuthSubjectService.currentTenant();
        rejectAlreadyBound(subject.accountId());
        ensureCipherConfigured();
        MobileProtectionService.ProtectedMobile protectedMobile =
                mobileProtectionService.protect(request.mobile());
        SmsChallengeVerificationResult verified = smsChallengeService.verifyAndConsume(
                request.challengeId(),
                request.code(),
                SmsPurpose.MOBILE_BIND
        );
        if (!MessageDigest.isEqual(
                verified.mobileHash().getBytes(StandardCharsets.US_ASCII),
                protectedMobile.hash().getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new BusinessException(CHALLENGE_MISMATCH_CODE, "验证码无效、已过期或已使用", 400);
        }
        MobileCipherService.EncryptedMobile encrypted =
                mobileCipherService.encrypt(protectedMobile.normalized());
        OffsetDateTime verifiedTime = OffsetDateTime.now();
        MobileBindingEntity entity = new MobileBindingEntity();
        entity.setSubjectType(AuthConstants.TENANT_ACCOUNT_SUBJECT);
        entity.setSubjectId(subject.accountId());
        entity.setMobileCiphertext(encrypted.ciphertext());
        entity.setMobileHash(protectedMobile.hash());
        entity.setMaskedMobile(protectedMobile.masked());
        entity.setEncryptionKeyId(encrypted.keyId());
        entity.setStatus("VERIFIED");
        entity.setVerifiedTime(verifiedTime);
        persistenceService.bind(entity, subject);
        return new MobileBindingStatusVO(true, protectedMobile.masked(), verifiedTime);
    }

    /** @param accountId 当前账号 ID，已有有效绑定时拒绝重复首次绑定 */
    private void rejectAlreadyBound(long accountId) {
        if (persistenceService.findVerified(AuthConstants.TENANT_ACCOUNT_SUBJECT, accountId) != null) {
            throw new BusinessException(ALREADY_BOUND_CODE, "当前账号已经绑定手机号", 409);
        }
    }

    /** 把密钥缺失转换成不泄露配置细节的稳定服务错误。 */
    private void ensureCipherConfigured() {
        try {
            mobileCipherService.ensureConfigured();
        } catch (IllegalStateException exception) {
            throw new BusinessException(
                    MOBILE_SECURITY_UNAVAILABLE_CODE,
                    "手机号安全服务暂时不可用",
                    503
            );
        }
    }
}
