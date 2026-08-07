package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.internal.client.IamPlatformIdentityClient;
import com.canteen.smile.internal.client.dto.TenantAccountActivationContextInternalResponse;
import com.canteen.smile.internal.dto.TenantActivationTicketInternalResponse;
import com.canteen.smile.modules.auth.entity.ActivationTicketEntity;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.mapper.ActivationTicketMapper;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import com.canteen.smile.modules.auth.vo.ActivationCompleteVO;
import com.canteen.smile.modules.auth.vo.ActivationContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

/** 租户账号一次性激活票据签发、校验和密码设置服务。 */
@Service
@RequiredArgsConstructor
public class TenantAccountActivationService {

    /** 租户账号认证主体。 */
    private static final String TENANT_ACCOUNT = "TENANT_ACCOUNT";

    /** 激活票据默认有效小时数。 */
    private static final long ACTIVATION_HOURS = 24;

    /** 无效激活票据错误码。 */
    private static final String INVALID_ACTIVATION_CODE = "AUTH_1021";

    /** 安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** 激活票据数据访问接口。 */
    private final ActivationTicketMapper activationTicketMapper;

    /** 凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /** 激活本地事务服务。 */
    private final ActivationTicketPersistenceService persistenceService;

    /** IAM 内部 Client。 */
    private final IamPlatformIdentityClient iamClient;

    /** 默认企业密码策略。 */
    private final PasswordPolicyService passwordPolicyService;

    /** Argon2id 密码编码器。 */
    private final PasswordEncoder passwordEncoder;

    /** @param accountId 租户账号 ID @return 仅展示一次的激活票据 */
    public TenantActivationTicketInternalResponse issue(long accountId) {
        TenantAccountActivationContextInternalResponse context = iamClient.activationContext(accountId);
        CredentialEntity credential = credentialMapper.selectBySubject(TENANT_ACCOUNT, accountId);
        if (context == null || !"PENDING_ACTIVATION".equals(context.status())
                || credential == null || !"PENDING".equals(credential.getStatus())) {
            throw invalidTicket();
        }
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawTicket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(ACTIVATION_HOURS);
        ActivationTicketEntity entity = new ActivationTicketEntity();
        entity.setSubjectType(TENANT_ACCOUNT);
        entity.setSubjectId(accountId);
        entity.setTicketHash(hash(rawTicket));
        entity.setStatus("ACTIVE");
        entity.setExpiresAt(expiresAt);
        persistenceService.replaceActiveTicket(entity);
        return new TenantActivationTicketInternalResponse(rawTicket, expiresAt);
    }

    /** @param rawTicket URL 中的原始票据 @return 激活页展示上下文 */
    public ActivationContextVO context(String rawTicket) {
        ActivationTicketEntity ticket = requireActiveTicket(rawTicket);
        TenantAccountActivationContextInternalResponse context = iamClient.activationContext(ticket.getSubjectId());
        return new ActivationContextVO(
                context.username(), context.displayName(), context.tenantName(), context.organizationName(), ticket.getExpiresAt()
        );
    }

    /**
     * 设置初始密码、消费票据并同步 IAM 账号状态。
     *
     * @param rawTicket URL 中的原始票据
     * @param rawPassword 当前请求中解密出的密码
     * @return 激活完成结果
     */
    public ActivationCompleteVO complete(String rawTicket, String rawPassword) {
        ActivationTicketEntity ticket = requireActiveTicket(rawTicket);
        TenantAccountActivationContextInternalResponse context = iamClient.activationContext(ticket.getSubjectId());
        passwordPolicyService.validate(rawPassword, context.username());
        String passwordHash = passwordEncoder.encode(rawPassword);
        persistenceService.activateCredential(ticket, passwordHash);
        TenantAccountActivationContextInternalResponse activated = iamClient.activateTenantAccount(ticket.getSubjectId());
        if (!"ACTIVE".equals(activated.status())) {
            throw new BusinessException("AUTH_1023", "账号资料激活暂未完成，请稍后重试", 502);
        }
        return new ActivationCompleteVO(activated.username(), "LOGIN");
    }

    /** @param rawTicket 原始票据 @return 有效票据实体 */
    private ActivationTicketEntity requireActiveTicket(String rawTicket) {
        ActivationTicketEntity ticket = activationTicketMapper.selectByHash(hash(rawTicket));
        if (ticket == null || !TENANT_ACCOUNT.equals(ticket.getSubjectType())
                || !"ACTIVE".equals(ticket.getStatus()) || !ticket.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw invalidTicket();
        }
        return ticket;
    }

    /** @param value 原始票据 @return SHA-256 摘要 */
    private String hash(String value) {
        return HmacRequestSigner.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /** @return 不泄露票据细节的统一异常 */
    private BusinessException invalidTicket() {
        return new BusinessException(INVALID_ACTIVATION_CODE, "激活链接无效、已过期或已使用", 400);
    }
}
