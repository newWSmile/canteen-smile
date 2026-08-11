package com.canteen.smile.modules.auth.service;

import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.model.ReauthAction;
import com.canteen.smile.modules.auth.vo.ReauthTicketVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

/** 为已经完成明确验证的主体统一签发五分钟单用途再认证票据。 */
@Service
@RequiredArgsConstructor
public class ReauthTicketIssueService {

    /** 再认证票据有效分钟数。 */
    private static final long REAUTH_MINUTES = 5L;

    /** 安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** 再认证票据事务服务。 */
    private final ReauthTicketPersistenceService persistenceService;

    /**
     * 签发仅允许一个敏感动作且只保存摘要的再认证票据。
     *
     * @param subjectType 已验证主体类型
     * @param subjectId 已验证主体 ID
     * @param allowedAction 票据唯一允许动作
     * @param verifyMethod 已完成的验证方式
     * @return 原始票据和绝对失效时间
     */
    public ReauthTicketVO issue(
            String subjectType,
            long subjectId,
            ReauthAction allowedAction,
            String verifyMethod
    ) {
        /** 只向当前验证成功调用方展示一次的原始票据。 */
        String rawTicket = randomTicket();
        /** 再认证票据绝对失效时间。 */
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(REAUTH_MINUTES);
        /** 仅保存摘要并绑定唯一敏感动作的票据实体。 */
        ReauthTicketEntity entity = new ReauthTicketEntity();
        entity.setSubjectType(subjectType);
        entity.setSubjectId(subjectId);
        entity.setTicketHash(hash(rawTicket));
        entity.setAllowedAction(allowedAction.name());
        entity.setVerifyMethod(verifyMethod);
        entity.setStatus(AuthConstants.ACTIVE_STATUS);
        entity.setExpiresAt(expiresAt);
        persistenceService.create(entity);
        return new ReauthTicketVO(rawTicket, expiresAt);
    }

    /** @return 256 位随机 URL 安全票据 */
    private String randomTicket() {
        /** 用于生成原始票据的 256 位安全随机数据。 */
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /** @param value 原始票据 @return SHA-256 小写十六进制摘要 */
    private String hash(String value) {
        return HmacRequestSigner.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }
}
