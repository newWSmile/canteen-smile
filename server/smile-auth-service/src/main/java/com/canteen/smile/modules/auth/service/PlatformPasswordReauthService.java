package com.canteen.smile.modules.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.model.ReauthAction;
import com.canteen.smile.modules.auth.vo.ReauthTicketVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

/** 当前平台身份通过当前密码获得五分钟敏感操作再认证票据。 */
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
        /** 当前登录平台身份 ID。 */
        long identityId = currentPlatformIdentityId();
        /** 当前平台身份凭证。 */
        CredentialEntity credential = credentialMapper.selectBySubject(
                AuthConstants.PLATFORM_IDENTITY_SUBJECT,
                identityId
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
        entity.setSubjectType(AuthConstants.PLATFORM_IDENTITY_SUBJECT);
        entity.setSubjectId(identityId);
        entity.setTicketHash(hash(rawTicket));
        entity.setAllowedAction(allowedAction.name());
        entity.setVerifyMethod(AuthConstants.PASSWORD_LOGIN_METHOD);
        entity.setStatus(AuthConstants.ACTIVE_STATUS);
        entity.setExpiresAt(expiresAt);
        persistenceService.create(entity);
        return new ReauthTicketVO(rawTicket, expiresAt);
    }

    /** @return 当前 Sa-Token 中的平台身份 ID */
    private long currentPlatformIdentityId() {
        /** 当前登录 ID。 */
        String loginId = String.valueOf(StpUtil.getLoginId());
        if (!loginId.startsWith(AuthConstants.PLATFORM_LOGIN_PREFIX)) {
            throw new BusinessException("AUTH_1202", "当前身份不是平台身份", 403);
        }
        try {
            return Long.parseLong(loginId.substring(AuthConstants.PLATFORM_LOGIN_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new BusinessException("AUTH_1202", "当前身份不是平台身份", 403);
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
}
