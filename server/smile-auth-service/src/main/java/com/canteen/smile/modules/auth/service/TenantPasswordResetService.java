package com.canteen.smile.modules.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.internal.client.IamPlatformIdentityClient;
import com.canteen.smile.internal.client.dto.TenantAccountActivationContextInternalResponse;
import com.canteen.smile.internal.dto.TenantPasswordResetTicketRequest;
import com.canteen.smile.internal.dto.TenantPasswordResetTicketResponse;
import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import com.canteen.smile.modules.auth.entity.CredentialEntity;
import com.canteen.smile.modules.auth.entity.PasswordResetTicketEntity;
import com.canteen.smile.modules.auth.entity.ReauthTicketEntity;
import com.canteen.smile.modules.auth.mapper.CredentialMapper;
import com.canteen.smile.modules.auth.mapper.PasswordResetTicketMapper;
import com.canteen.smile.modules.auth.mapper.ReauthTicketMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.model.ReauthAction;
import com.canteen.smile.modules.auth.vo.PasswordResetCompleteVO;
import com.canteen.smile.modules.auth.vo.PasswordResetContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

/** 租户账号一次性密码恢复票据签发、校验和密码更新服务。 */
@Service
@RequiredArgsConstructor
public class TenantPasswordResetService {

    /** 一次性恢复链接有效分钟数。 */
    private static final long RESET_MINUTES = 30;

    /** 无效恢复票据错误码。 */
    private static final String INVALID_RESET_CODE = "AUTH_1204";

    /** 管理员线下交付的一次性链接重置模式。 */
    private static final String ONE_TIME_LINK_MODE = "ONE_TIME_LINK";

    /** 手机号验证码自助重置模式。 */
    private static final String SMS_RESET_MODE = "SMS";

    /** 安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** 再认证票据数据访问接口。 */
    private final ReauthTicketMapper reauthTicketMapper;

    /** 密码重置票据数据访问接口。 */
    private final PasswordResetTicketMapper resetTicketMapper;

    /** 认证凭证数据访问接口。 */
    private final CredentialMapper credentialMapper;

    /** 密码恢复本地事务服务。 */
    private final PasswordResetPersistenceService persistenceService;

    /** IAM 内部 Client。 */
    private final IamPlatformIdentityClient iamClient;

    /** 默认企业密码策略。 */
    private final PasswordPolicyService passwordPolicyService;

    /** 密码历史复用校验服务。 */
    private final PasswordHistoryService passwordHistoryService;

    /** Argon2id 密码编码器。 */
    private final PasswordEncoder passwordEncoder;

    /**
     * 校验平台再认证票据并为租户账号生成一次性恢复链接。
     *
     * @param accountId 租户账号 ID
     * @param request IAM 已完成权限和目标边界校验的内部请求
     * @return 只展示一次的恢复票据
     */
    public TenantPasswordResetTicketResponse issue(
            long accountId,
            TenantPasswordResetTicketRequest request
    ) {
        if (request.allowedAction() != ReauthAction.TENANT_OWNER_PASSWORD_RESET
                && request.allowedAction() != ReauthAction.TENANT_USER_PASSWORD_RESET) {
            throw invalidReauth();
        }
        /** 发起平台身份 ID。 */
        long initiatorId = Long.parseLong(request.initiatorId());
        /** 匹配原始票据摘要的再认证记录。 */
        ReauthTicketEntity reauthTicket = reauthTicketMapper.selectByHash(hash(request.reauthTicket()));
        String expectedSubjectType = request.allowedAction() == ReauthAction.TENANT_OWNER_PASSWORD_RESET
                ? AuthConstants.PLATFORM_IDENTITY_SUBJECT
                : AuthConstants.TENANT_ACCOUNT_SUBJECT;
        if (!expectedSubjectType.equals(request.initiatorType())
                || reauthTicket == null
                || !expectedSubjectType.equals(reauthTicket.getSubjectType())
                || reauthTicket.getSubjectId() == null
                || reauthTicket.getSubjectId().longValue() != initiatorId
                || !request.allowedAction().name().equals(reauthTicket.getAllowedAction())
                || !AuthConstants.ACTIVE_STATUS.equals(reauthTicket.getStatus())
                || !reauthTicket.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw invalidReauth();
        }
        /** IAM 中已进入待重置状态的账号上下文。 */
        TenantAccountActivationContextInternalResponse context = iamClient.activationContext(accountId);
        /** Auth 中的租户账号凭证。 */
        CredentialEntity credential = credentialMapper.selectBySubject(
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                accountId
        );
        if (!isResetSynchronizationStatus(context.status())
                || credential == null
                || !(AuthConstants.ACTIVE_STATUS.equals(credential.getStatus())
                || "RESET_REQUIRED".equals(credential.getStatus()))) {
            throw invalidReset();
        }
        /** 原始高熵一次性密码恢复票据。 */
        String rawTicket = randomTicket();
        /** 密码恢复票据绝对失效时间。 */
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(RESET_MINUTES);
        /** 待持久化密码恢复票据。 */
        PasswordResetTicketEntity resetTicket = new PasswordResetTicketEntity();
        resetTicket.setSubjectType(AuthConstants.TENANT_ACCOUNT_SUBJECT);
        resetTicket.setSubjectId(accountId);
        resetTicket.setResetMode(ONE_TIME_LINK_MODE);
        resetTicket.setTicketHash(hash(rawTicket));
        resetTicket.setInitiatedByType(request.initiatorType());
        resetTicket.setInitiatedById(initiatorId);
        resetTicket.setStatus(AuthConstants.ACTIVE_STATUS);
        resetTicket.setExpiresAt(expiresAt);
        persistenceService.initiate(reauthTicket, resetTicket);
        StpUtil.logout(AuthConstants.TENANT_LOGIN_PREFIX + accountId);
        return new TenantPasswordResetTicketResponse(rawTicket, expiresAt);
    }

    /**
     * 为已通过 PASSWORD_RESET 用途短信校验的账号签发自助找回票据。
     *
     * @param accountId 已经 Auth 绑定与 IAM 状态双重校验的账号 ID
     * @param selectorTicket 多账号时已校验的选择票据；单账号时为空
     * @return 仅向当前调用方展示一次的密码重置票据
     */
    public String issueSmsSelfService(
            long accountId,
            AccountSelectorTicketEntity selectorTicket
    ) {
        /** 与 IAM 候选账号对应且当前可用于登录的 Auth 凭证。 */
        CredentialEntity credential = credentialMapper.selectBySubject(
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                accountId
        );
        if (credential == null || !AuthConstants.ACTIVE_STATUS.equals(credential.getStatus())) {
            throw invalidReset();
        }
        /** 只向已验证手机号持有人展示一次的原始重置票据。 */
        String rawTicket = randomTicket();
        /** 仅保存摘要并固定为 SMS 自助模式的重置记录。 */
        PasswordResetTicketEntity resetTicket = new PasswordResetTicketEntity();
        resetTicket.setSubjectType(AuthConstants.TENANT_ACCOUNT_SUBJECT);
        resetTicket.setSubjectId(accountId);
        resetTicket.setResetMode(SMS_RESET_MODE);
        resetTicket.setTicketHash(hash(rawTicket));
        resetTicket.setInitiatedByType("SELF");
        resetTicket.setInitiatedById(accountId);
        resetTicket.setStatus(AuthConstants.ACTIVE_STATUS);
        resetTicket.setExpiresAt(OffsetDateTime.now().plusMinutes(RESET_MINUTES));
        persistenceService.initiateSmsSelfService(resetTicket, selectorTicket);
        return rawTicket;
    }

    /** @param rawTicket 原始恢复票据 @return 恢复页面展示上下文 */
    public PasswordResetContextVO context(String rawTicket) {
        PasswordResetTicketEntity ticket = requireActiveTicket(rawTicket);
        TenantAccountActivationContextInternalResponse context = iamClient.activationContext(ticket.getSubjectId());
        CredentialEntity credential = credentialMapper.selectBySubject(
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                ticket.getSubjectId()
        );
        if (!matchesResetState(ticket, context, credential)) {
            throw invalidReset();
        }
        return new PasswordResetContextVO(
                context.username(),
                context.displayName(),
                context.tenantName(),
                context.organizationName(),
                ticket.getExpiresAt()
        );
    }

    /**
     * 校验新密码、同步 IAM 状态并原子更新 Auth 凭证。
     *
     * @param rawTicket 原始恢复票据
     * @param rawPassword 当前请求解密出的新密码
     * @return 密码恢复完成结果
     */
    public PasswordResetCompleteVO complete(String rawTicket, String rawPassword) {
        PasswordResetTicketEntity ticket = requireActiveTicket(rawTicket);
        TenantAccountActivationContextInternalResponse context = iamClient.activationContext(ticket.getSubjectId());
        CredentialEntity credential = credentialMapper.selectBySubject(
                AuthConstants.TENANT_ACCOUNT_SUBJECT,
                ticket.getSubjectId()
        );
        if (!matchesResetState(ticket, context, credential)) {
            throw invalidReset();
        }
        passwordPolicyService.validate(rawPassword, context.username());
        passwordHistoryService.validateNotReused(rawPassword, credential);
        /** 新 Argon2id 密码摘要。 */
        String passwordHash = passwordEncoder.encode(rawPassword);
        if (SMS_RESET_MODE.equals(ticket.getResetMode())) {
            persistenceService.completeSmsSelfService(ticket, passwordHash, context);
            StpUtil.logout(AuthConstants.TENANT_LOGIN_PREFIX + ticket.getSubjectId());
            return new PasswordResetCompleteVO(context.username(), "LOGIN");
        }
        TenantAccountActivationContextInternalResponse completed =
                iamClient.completeTenantAccountPasswordReset(ticket.getSubjectId());
        if (!AuthConstants.ACTIVE_STATUS.equals(completed.status())) {
            throw new BusinessException("AUTH_1206", "账号资料恢复暂未完成，请稍后重试", 502);
        }
        persistenceService.complete(ticket, passwordHash);
        return new PasswordResetCompleteVO(completed.username(), "LOGIN");
    }

    /** @param rawTicket 原始恢复票据 @return 有效恢复票据实体 */
    private PasswordResetTicketEntity requireActiveTicket(String rawTicket) {
        PasswordResetTicketEntity ticket = resetTicketMapper.selectByHash(hash(rawTicket));
        if (ticket == null
                || !AuthConstants.TENANT_ACCOUNT_SUBJECT.equals(ticket.getSubjectType())
                || !(ONE_TIME_LINK_MODE.equals(ticket.getResetMode())
                || SMS_RESET_MODE.equals(ticket.getResetMode()))
                || !AuthConstants.ACTIVE_STATUS.equals(ticket.getStatus())
                || !ticket.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw invalidReset();
        }
        return ticket;
    }

    /**
     * IAM 可能已经在前一次跨服务尝试中恢复为有效状态；Auth 本地票据和凭证仍是最终约束。
     *
     * @param status IAM 账号状态
     * @return 是否允许继续完成当前恢复票据
     */
    private boolean isResetSynchronizationStatus(String status) {
        return "PASSWORD_RESET_REQUIRED".equals(status) || AuthConstants.ACTIVE_STATUS.equals(status);
    }

    /**
     * 不同重置模式必须匹配各自的 IAM 与 Auth 凭证状态。
     *
     * @param ticket 当前恢复票据
     * @param context IAM 账号上下文
     * @param credential Auth 凭证
     * @return 是否允许展示或完成当前恢复流程
     */
    private boolean matchesResetState(
            PasswordResetTicketEntity ticket,
            TenantAccountActivationContextInternalResponse context,
            CredentialEntity credential
    ) {
        if (credential == null) return false;
        if (SMS_RESET_MODE.equals(ticket.getResetMode())) {
            return AuthConstants.ACTIVE_STATUS.equals(context.status())
                    && AuthConstants.ACTIVE_STATUS.equals(credential.getStatus());
        }
        return ONE_TIME_LINK_MODE.equals(ticket.getResetMode())
                && isResetSynchronizationStatus(context.status())
                && "RESET_REQUIRED".equals(credential.getStatus());
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

    /** @return 不泄露详情的再认证票据异常 */
    private BusinessException invalidReauth() {
        return new BusinessException("AUTH_1203", "再认证票据无效、已过期或用途不匹配", 401);
    }

    /** @return 不泄露详情的密码恢复票据异常 */
    private BusinessException invalidReset() {
        return new BusinessException(INVALID_RESET_CODE, "密码恢复链接无效、已过期或已使用", 400);
    }
}
