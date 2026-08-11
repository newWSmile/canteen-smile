package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import com.canteen.smile.modules.auth.mapper.AccountSelectorTicketMapper;
import com.canteen.smile.modules.auth.model.AccountSelectorFlow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

/** 手机号验证后多账号选择票据的本地事务边界。 */
@Service
@RequiredArgsConstructor
public class AccountSelectorTicketPersistenceService {

    /** 账号选择票据无效错误码。 */
    private static final String INVALID_SELECTOR_TICKET_CODE = "AUTH_1014";

    /** 账号选择票据数据访问接口。 */
    private final AccountSelectorTicketMapper mapper;

    /** 生成不可预测账号选择票据的安全随机源。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 创建只保存摘要并绑定唯一流程的短期账号选择票据。
     *
     * @param mobileHash 已验证手机号摘要
     * @param candidateDigest 候选账号集合摘要
     * @param appCode 应用入口
     * @param flow 票据唯一允许流程
     * @param expiresAt 绝对失效时间
     * @return 只向当前调用方返回一次的原始票据
     */
    @Transactional
    public String issue(
            String mobileHash,
            String candidateDigest,
            String appCode,
            AccountSelectorFlow flow,
            OffsetDateTime expiresAt
    ) {
        /** 用于生成原始票据的 256 位随机数据。 */
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        /** 只向当前调用方展示一次的 URL 安全原始票据。 */
        String rawTicket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        /** 仅保存摘要、候选集合与唯一流程的账号选择票据。 */
        AccountSelectorTicketEntity entity = new AccountSelectorTicketEntity();
        entity.setTicketHash(hash(rawTicket));
        entity.setMobileHash(mobileHash);
        entity.setCandidateDigest(candidateDigest);
        entity.setAppCode(appCode);
        entity.setFlowType(flow.name());
        entity.setExpiresAt(expiresAt);
        create(entity);
        return rawTicket;
    }

    /** @param entity 已绑定唯一流程且待持久化的票据摘要 */
    private void create(AccountSelectorTicketEntity entity) {
        if (mapper.insert(entity) != 1) {
            throw new IllegalStateException("Account selector ticket was not inserted");
        }
    }

    /** @param ticketHash 原始票据摘要 @return 当前有效票据 */
    private AccountSelectorTicketEntity findActiveByHash(String ticketHash) {
        /** 与摘要匹配且仍处于有效期内的票据。 */
        AccountSelectorTicketEntity entity = mapper.selectActiveByHash(ticketHash);
        if (entity == null) throw invalidTicket();
        return entity;
    }

    /**
     * 校验原始票据摘要以及唯一允许流程。
     *
     * @param rawTicket 原始账号选择票据
     * @param expectedFlow 当前业务要求的票据流程
     * @return 当前有效且用途匹配的票据
     */
    @Transactional(readOnly = true)
    public AccountSelectorTicketEntity requireActive(
            String rawTicket,
            AccountSelectorFlow expectedFlow
    ) {
        /** 摘要、状态和有效期均有效的账号选择票据。 */
        AccountSelectorTicketEntity entity = findActiveByHash(hash(rawTicket));
        if (!expectedFlow.name().equals(entity.getFlowType())) throw invalidTicket();
        return entity;
    }

    /** @param entity 已完成全部业务校验的票据 */
    @Transactional
    public void consume(AccountSelectorTicketEntity entity) {
        if (mapper.consume(entity.getId(), entity.getVersion()) != 1) throw invalidTicket();
    }

    /** @return 不泄露票据状态细节的统一异常 */
    private BusinessException invalidTicket() {
        return new BusinessException(INVALID_SELECTOR_TICKET_CODE, "账号选择凭证无效或已过期", 400);
    }

    /** @param value 原始票据 @return SHA-256 小写十六进制摘要 */
    private String hash(String value) {
        return HmacRequestSigner.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }
}
