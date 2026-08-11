package com.canteen.smile.modules.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.auth.dto.CurrentMobileChallengeRequest;
import com.canteen.smile.modules.auth.dto.CurrentMobileVerificationRequest;
import com.canteen.smile.modules.auth.dto.MobileBindingChallengeRequest;
import com.canteen.smile.modules.auth.dto.MobileBindingConfirmRequest;
import com.canteen.smile.modules.auth.dto.MobileChangeChallengeRequest;
import com.canteen.smile.modules.auth.dto.MobileChangeConfirmRequest;
import com.canteen.smile.modules.auth.dto.MobileUnbindConfirmRequest;
import com.canteen.smile.modules.auth.entity.MobileBindingEntity;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.model.ReauthAction;
import com.canteen.smile.modules.auth.vo.MobileBindingStatusVO;
import com.canteen.smile.modules.auth.vo.ReauthTicketVO;
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

    /** 当前账号尚未绑定手机号的错误码。 */
    private static final String NOT_BOUND_CODE = "AUTH_1302";

    /** 新手机号与当前手机号相同的错误码。 */
    private static final String SAME_MOBILE_CODE = "AUTH_1303";

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

    /** 已完成验证后的统一再认证票据签发服务。 */
    private final ReauthTicketIssueService reauthTicketIssueService;

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
    @AuditOperation(
            source = "AUTH",
            categoryPath = {"租户端", "账号安全", "手机号凭证"},
            actionCode = "auth:mobile:bind",
            actionName = "绑定手机号",
            targetType = "TENANT_ACCOUNT",
            targetId = "#actor.operatorId",
            targetName = "#actor.displayName",
            targetCode = "#actor.username",
            maskedMobile = "#result?.maskedMobile"
    )
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
        /** 新手机号完成验证的服务端时间。 */
        OffsetDateTime verifiedTime = OffsetDateTime.now();
        /** 当前账号首次写入的已验证手机号绑定。 */
        MobileBindingEntity entity = bindingEntity(subject.accountId(), protectedMobile, verifiedTime);
        persistenceService.bind(entity, subject);
        return new MobileBindingStatusVO(true, protectedMobile.masked(), verifiedTime);
    }

    /**
     * 向当前数据库中已验证的手机号发送换绑或解绑验证码。
     *
     * @param request 当前设备限流上下文
     * @param clientIp 服务端取得的来源 IP
     * @return 不泄露完整手机号的挑战摘要
     */
    public SmsChallengeVO createCurrentMobileChallenge(
            CurrentMobileChallengeRequest request,
            String clientIp
    ) {
        /** 当前可信租户账号。 */
        CurrentAuthSubjectService.CurrentTenantSubject subject =
                currentAuthSubjectService.currentTenant();
        /** 当前账号唯一有效的手机号绑定。 */
        MobileBindingEntity currentBinding = requireVerifiedBinding(subject.accountId());
        ensureCipherConfigured();
        /** 仅在当前 Auth 调用栈内短暂存在的当前完整手机号。 */
        String currentMobile = decryptCurrentMobile(currentBinding);
        return smsChallengeService.create(
                challengeRequest(
                        currentMobile,
                        request.getDeviceId(),
                        request.getCaptchaTicket(),
                        SmsPurpose.MOBILE_CHANGE
                ),
                clientIp
        );
    }

    /**
     * 校验当前手机号验证码并签发仅允许换绑或解绑的五分钟票据。
     *
     * @param request 当前手机号挑战、验证码和唯一允许动作
     * @return 仅展示一次的再认证票据
     */
    public ReauthTicketVO verifyCurrentMobile(CurrentMobileVerificationRequest request) {
        requireMobileAction(request.allowedAction());
        /** 当前可信租户账号。 */
        CurrentAuthSubjectService.CurrentTenantSubject subject =
                currentAuthSubjectService.currentTenant();
        /** 当前账号唯一有效的手机号绑定。 */
        MobileBindingEntity currentBinding = requireVerifiedBinding(subject.accountId());
        /** 已原子消费且用途确认为 MOBILE_CHANGE 的短信验证结果。 */
        SmsChallengeVerificationResult verified = smsChallengeService.verifyAndConsume(
                request.challengeId(), request.code(), SmsPurpose.MOBILE_CHANGE
        );
        requireMatchingHash(verified.mobileHash(), currentBinding.getMobileHash());
        return reauthTicketIssueService.issue(
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                subject.accountId(),
                request.allowedAction(),
                AuthConstants.SMS_LOGIN_METHOD
        );
    }

    /**
     * 向与当前绑定不同的新手机号发送换绑验证码。
     *
     * @param request 新手机号和设备限流上下文
     * @param clientIp 服务端取得的来源 IP
     * @return 新手机号脱敏挑战摘要
     */
    public SmsChallengeVO createChangeChallenge(
            MobileChangeChallengeRequest request,
            String clientIp
    ) {
        /** 当前可信租户账号。 */
        CurrentAuthSubjectService.CurrentTenantSubject subject =
                currentAuthSubjectService.currentTenant();
        /** 当前账号唯一有效的手机号绑定。 */
        MobileBindingEntity currentBinding = requireVerifiedBinding(subject.accountId());
        persistenceService.validateReauth(
                request.reauthTicket(), subject, ReauthAction.MOBILE_CHANGE
        );
        ensureCipherConfigured();
        /** 待换绑新手机号的摘要和脱敏投影。 */
        MobileProtectionService.ProtectedMobile newMobile =
                mobileProtectionService.protect(request.mobile());
        rejectSameMobile(currentBinding, newMobile);
        return smsChallengeService.create(
                challengeRequest(
                        newMobile.normalized(),
                        request.deviceId(),
                        request.captchaTicket(),
                        SmsPurpose.MOBILE_CHANGE
                ),
                clientIp
        );
    }

    /**
     * 使用再认证票据和新手机号验证码原子完成换绑并下线全部设备。
     *
     * @param request 再认证票据和新手机号验证信息
     * @return 新的脱敏绑定状态
     */
    @AuditOperation(
            source = "AUTH",
            categoryPath = {"租户端", "账号安全", "手机号凭证"},
            actionCode = "auth:mobile:change",
            actionName = "更换手机号",
            targetType = "TENANT_ACCOUNT",
            targetId = "#actor.operatorId",
            targetName = "#actor.displayName",
            targetCode = "#actor.username",
            maskedMobile = "#result?.maskedMobile"
    )
    public MobileBindingStatusVO change(MobileChangeConfirmRequest request) {
        /** 当前可信租户账号。 */
        CurrentAuthSubjectService.CurrentTenantSubject subject =
                currentAuthSubjectService.currentTenant();
        /** 当前账号唯一有效的旧手机号绑定。 */
        MobileBindingEntity currentBinding = requireVerifiedBinding(subject.accountId());
        persistenceService.validateReauth(request.reauthTicket(), subject, ReauthAction.MOBILE_CHANGE);
        ensureCipherConfigured();
        /** 待换绑新手机号的摘要和脱敏投影。 */
        MobileProtectionService.ProtectedMobile newMobile =
                mobileProtectionService.protect(request.newMobile());
        rejectSameMobile(currentBinding, newMobile);
        /** 已原子消费且用途确认为 MOBILE_CHANGE 的新手机号验证结果。 */
        SmsChallengeVerificationResult verified = smsChallengeService.verifyAndConsume(
                request.newChallengeId(), request.newCode(), SmsPurpose.MOBILE_CHANGE
        );
        requireMatchingHash(verified.mobileHash(), newMobile.hash());
        /** 新手机号完成验证的服务端时间。 */
        OffsetDateTime verifiedTime = OffsetDateTime.now();
        /** 待原子替换写入的新手机号绑定。 */
        MobileBindingEntity newBinding = bindingEntity(
                subject.accountId(), newMobile, verifiedTime
        );
        persistenceService.change(currentBinding, newBinding, request.reauthTicket(), subject);
        logoutAllDevices(subject.accountId());
        return new MobileBindingStatusVO(true, newMobile.masked(), verifiedTime);
    }

    /**
     * 使用再认证票据原子解绑当前手机号并下线全部设备。
     *
     * @param request 仅允许解绑动作的一次性再认证票据
     * @return 未绑定状态
     */
    @AuditOperation(
            source = "AUTH",
            categoryPath = {"租户端", "账号安全", "手机号凭证"},
            actionCode = "auth:mobile:unbind",
            actionName = "解绑手机号",
            targetType = "TENANT_ACCOUNT",
            targetId = "#actor.operatorId",
            targetName = "#actor.displayName",
            targetCode = "#actor.username"
    )
    public MobileBindingStatusVO unbind(MobileUnbindConfirmRequest request) {
        /** 当前可信租户账号。 */
        CurrentAuthSubjectService.CurrentTenantSubject subject =
                currentAuthSubjectService.currentTenant();
        /** 当前账号唯一有效的手机号绑定。 */
        MobileBindingEntity currentBinding = requireVerifiedBinding(subject.accountId());
        persistenceService.unbind(currentBinding, request.reauthTicket(), subject);
        logoutAllDevices(subject.accountId());
        return MobileBindingStatusVO.unbound();
    }

    /** 创建只在当前服务调用栈内传给短信服务的挑战请求。 */
    private SmsChallengeCreateRequest challengeRequest(
            String mobile,
            String deviceId,
            String captchaTicket,
            SmsPurpose purpose
    ) {
        /** 待创建的短信验证码挑战。 */
        SmsChallengeCreateRequest request = new SmsChallengeCreateRequest();
        request.setPurpose(purpose);
        request.setMobile(mobile);
        request.setDeviceId(deviceId);
        request.setCaptchaTicket(captchaTicket);
        return request;
    }

    /** 创建已验证并加密的手机号绑定实体。 */
    private MobileBindingEntity bindingEntity(
            long accountId,
            MobileProtectionService.ProtectedMobile protectedMobile,
            OffsetDateTime verifiedTime
    ) {
        /** 使用当前版本密钥生成的手机号认证密文。 */
        MobileCipherService.EncryptedMobile encrypted =
                mobileCipherService.encrypt(protectedMobile.normalized());
        /** 仅映射 Auth 自有手机号绑定表的新实体。 */
        MobileBindingEntity entity = new MobileBindingEntity();
        entity.setSubjectType(AuthConstants.TENANT_ACCOUNT_SUBJECT);
        entity.setSubjectId(accountId);
        entity.setMobileCiphertext(encrypted.ciphertext());
        entity.setMobileHash(protectedMobile.hash());
        entity.setMaskedMobile(protectedMobile.masked());
        entity.setEncryptionKeyId(encrypted.keyId());
        entity.setStatus("VERIFIED");
        entity.setVerifiedTime(verifiedTime);
        return entity;
    }

    /** @param accountId 当前账号 ID @return 当前唯一有效手机号绑定 */
    private MobileBindingEntity requireVerifiedBinding(long accountId) {
        /** 数据库中当前唯一有效手机号绑定。 */
        MobileBindingEntity binding = persistenceService.findVerified(
                AuthConstants.TENANT_ACCOUNT_SUBJECT, accountId
        );
        if (binding == null) {
            throw new BusinessException(NOT_BOUND_CODE, "当前账号尚未绑定手机号", 409);
        }
        return binding;
    }

    /** 解密当前绑定并统一隐藏密钥配置和密文内部错误。 */
    private String decryptCurrentMobile(MobileBindingEntity binding) {
        try {
            return mobileCipherService.decrypt(
                    binding.getMobileCiphertext(), binding.getEncryptionKeyId()
            );
        } catch (IllegalStateException exception) {
            throw mobileSecurityUnavailable();
        }
    }

    /** 只允许手机号换绑与解绑动作使用当前手机号短信再认证。 */
    private void requireMobileAction(ReauthAction action) {
        if (action != ReauthAction.MOBILE_CHANGE && action != ReauthAction.MOBILE_UNBIND) {
            throw new BusinessException("AUTH_1202", "当前身份不能执行该敏感操作", 403);
        }
    }

    /** 使用恒定时间比较确保短信挑战属于预期手机号。 */
    private void requireMatchingHash(String actualHash, String expectedHash) {
        if (!MessageDigest.isEqual(
                actualHash.getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new BusinessException(CHALLENGE_MISMATCH_CODE, "验证码无效、已过期或已使用", 400);
        }
    }

    /** 拒绝把当前手机号重新换绑为自己。 */
    private void rejectSameMobile(
            MobileBindingEntity currentBinding,
            MobileProtectionService.ProtectedMobile newMobile
    ) {
        if (MessageDigest.isEqual(
                currentBinding.getMobileHash().getBytes(StandardCharsets.US_ASCII),
                newMobile.hash().getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new BusinessException(SAME_MOBILE_CODE, "新手机号不能与当前手机号相同", 409);
        }
    }

    /** 使 Sa-Token 中该账号全部设备会话立即失效。 */
    private void logoutAllDevices(long accountId) {
        StpUtil.logout(AuthConstants.TENANT_LOGIN_PREFIX + accountId);
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
            throw mobileSecurityUnavailable();
        }
    }

    /** @return 不泄露密钥版本和密文细节的手机号安全服务异常 */
    private BusinessException mobileSecurityUnavailable() {
        return new BusinessException(
                MOBILE_SECURITY_UNAVAILABLE_CODE,
                "手机号安全服务暂时不可用",
                503
        );
    }
}
