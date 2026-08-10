package com.canteen.smile.modules.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.model.ReauthAction;
import com.canteen.smile.modules.auth.vo.ReauthTicketVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

/** 当前平台或租户身份通过当前密码获得五分钟敏感操作再认证票据。 */
@Service
@RequiredArgsConstructor
public class PlatformPasswordReauthService {

    /** 再认证票据有效分钟数。 */
    private static final long REAUTH_MINUTES = 5;

    /** 再认证失败稳定错误码。 */
    private static final String REAUTH_FAILED_CODE = "AUTH_1201";

    /** 安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** 认证凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /** 恒定时间密码校验服务。 */
    private final PasswordVerificationService passwordVerificationService;

    /** 再认证票据事务服务。 */
    private final ReauthTicketPersistenceService persistenceService;

    /**
     * 校验当前平台身份密码并签发只允许单个操作的一次性票据。
     *
     * @param rawPassword 当前请求解密出的密码
     * @param allowedAction 票据允许执行的敏感操作
     * @return 原始票据及失效时间
     */
    public ReauthTicketVO issue(String rawPassword, ReauthAction allowedAction) {
        /** 当前登录身份。 */
        ReauthSubject subject = currentSubject();
        validateAllowedAction(subject.subjectType(), allowedAction);
        /** 当前身份凭证。 */
        CredentialEntity credential = credentialMapper.selectBySubject(
                subject.subjectType(),
                subject.subjectId()
        );
        if (!passwordVerificationService.matches(rawPassword, credential)) {
            throw new BusinessException(REAUTH_FAILED_CODE, "当前密码验证失败", 401);
        }
        /** 原始高熵一次性再认证票据。 */
        String rawTicket = randomTicket();
        /** 再认证票据绝对失效时间。 */
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(REAUTH_MINUTES);
        /** 待持久化票据摘要实体。 */
        ReauthTicketEntity entity = new ReauthTicketEntity();
        entity.setSubjectType(subject.subjectType());
        entity.setSubjectId(subject.subjectId());
        entity.setTicketHash(hash(rawTicket));
        entity.setAllowedAction(allowedAction.name());
        entity.setVerifyMethod(AuthConstants.PASSWORD_LOGIN_METHOD);
        entity.setStatus(AuthConstants.ACTIVE_STATUS);
        entity.setExpiresAt(expiresAt);
        persistenceService.create(entity);
        return new ReauthTicketVO(rawTicket, expiresAt);
    }

    /** @return 当前登录身份对应的密码信封用途 */
    public PasswordEnvelopePurpose currentPasswordPurpose() {
        return AuthConstants.PLATFORM_IDENTITY_SUBJECT.equals(currentSubject().subjectType())
                ? PasswordEnvelopePurpose.PLATFORM_REAUTH_PASSWORD
                : PasswordEnvelopePurpose.TENANT_REAUTH_PASSWORD;
    }

    /** @return 当前 Sa-Token 中的平台或租户身份 */
    private ReauthSubject currentSubject() {
        /** 当前登录 ID。 */
        String loginId = String.valueOf(StpUtil.getLoginId());
        String subjectType;
        String subjectIdText;
        if (loginId.startsWith(AuthConstants.PLATFORM_LOGIN_PREFIX)) {
            subjectType = AuthConstants.PLATFORM_IDENTITY_SUBJECT;
            subjectIdText = loginId.substring(AuthConstants.PLATFORM_LOGIN_PREFIX.length());
        } else if (loginId.startsWith(AuthConstants.TENANT_LOGIN_PREFIX)) {
            subjectType = AuthConstants.TENANT_ACCOUNT_SUBJECT;
            subjectIdText = loginId.substring(AuthConstants.TENANT_LOGIN_PREFIX.length());
        } else {
            throw new BusinessException("AUTH_1202", "当前身份不支持密码再认证", 403);
        }
        try {
            return new ReauthSubject(subjectType, Long.parseLong(subjectIdText));
        } catch (NumberFormatException exception) {
            throw new BusinessException("AUTH_1202", "当前身份不支持密码再认证", 403);
        }
    }

    /** 校验身份类型只能签发自身允许的敏感动作。 */
    private void validateAllowedAction(String subjectType, ReauthAction action) {
        boolean platformAction = action == ReauthAction.TENANT_OWNER_PASSWORD_RESET;
        if (platformAction != AuthConstants.PLATFORM_IDENTITY_SUBJECT.equals(subjectType)) {
            throw new BusinessException("AUTH_1202", "当前身份不能执行该敏感操作", 403);
        }
    }

    /** @return 256 位随机 URL 安全票据 */
    private String randomTicket() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /** @param value 原始票据 @return SHA-256 摘要 */
    private String hash(String value) {
        return HmacRequestSigner.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /** 当前再认证主体。 */
    private record ReauthSubject(String subjectType, long subjectId) {
    }
}
