package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import com.canteen.smile.internal.client.IamPlatformIdentityClient;
import com.canteen.smile.internal.client.dto.MobileAccountLoginCandidateInternalResponse;
import com.canteen.smile.internal.client.dto.MobileAccountLoginResolutionInternalRequest;
import com.canteen.smile.modules.auth.dto.AccountSelectionLoginRequest;
import com.canteen.smile.modules.auth.dto.DeviceRequest;
import com.canteen.smile.modules.auth.dto.SmsLoginRequest;
import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import com.canteen.smile.modules.auth.mapper.MobileBindingMapper;
import com.canteen.smile.modules.auth.model.AuthConstants;
import com.canteen.smile.modules.auth.vo.LoginResultVO;
import com.canteen.smile.modules.auth.vo.MobileLoginCandidateVO;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.service.SmsChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 手机号验证码登录、候选账号选择和设备会话建立服务。 */
@Service
@RequiredArgsConstructor
public class TenantSmsLoginService {

    /** 手机号没有当前入口可登录账号时使用的稳定错误码。 */
    private static final String NO_AVAILABLE_ACCOUNT_CODE = "AUTH_1015";

    /** 账号选择票据校验失败错误码。 */
    private static final String INVALID_SELECTOR_TICKET_CODE = "AUTH_1014";

    /** 账号选择票据有效秒数。 */
    private static final long SELECTOR_TICKET_TTL_SECONDS = 300L;

    /** 手机号验证码挑战服务。 */
    private final SmsChallengeService smsChallengeService;

    /** 手机号绑定数据访问接口。 */
    private final MobileBindingMapper mobileBindingMapper;

    /** IAM 登录候选解析 Client。 */
    private final IamPlatformIdentityClient iamClient;

    /** 设备会话数据访问接口。 */
    private final DeviceSessionMapper deviceSessionMapper;

    /** 账号选择票据事务服务。 */
    private final AccountSelectorTicketPersistenceService ticketPersistenceService;

    /** 租户设备会话服务。 */
    private final TenantSessionService tenantSessionService;

    /** 生成不可预测账号选择票据的安全随机源。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 验证 LOGIN 用途短信验证码；单账号直接登录，多账号返回短期选择票据。
     *
     * @param request 短信验证码登录请求
     * @param loginIp 服务端取得的来源 IP
     * @return 已认证会话或账号选择结果
     */
    public LoginResultVO login(SmsLoginRequest request, String loginIp) {
        requireTenantApp(request.getAppCode());
        SmsChallengeVerificationResult verification = smsChallengeService.verifyAndConsume(
                request.getChallengeId(), request.getCode(), SmsPurpose.LOGIN
        );
        List<ResolvedCandidate> candidates = resolveCandidates(
                request.getAppCode(), verification.mobileHash()
        );
        if (candidates.isEmpty()) throw noAvailableAccount();
        if (candidates.size() == 1) {
            return LoginResultVO.authenticated(createSession(
                    candidates.get(0).identity(), request.isRememberMe(), request.getDevice(), loginIp,
                    request.getAppCode()
            ));
        }
        String rawTicket = issueSelectorTicket(
                verification.mobileHash(), request.getAppCode(), candidates
        );
        return LoginResultVO.accountSelectionRequired(
                rawTicket,
                candidates.stream().map(this::externalCandidate).toList()
        );
    }

    /**
     * 校验短期选择票据和候选集合完整性后，以用户选择的账号建立会话。
     *
     * @param request 账号选择请求
     * @param loginIp 服务端取得的来源 IP
     * @return 已认证设备会话结果
     */
    public LoginResultVO selectAccount(AccountSelectionLoginRequest request, String loginIp) {
        requireTenantApp(request.getAppCode());
        AccountSelectorTicketEntity ticket = ticketPersistenceService.requireActive(
                hash(request.getAccountSelectorTicket())
        );
        if (!request.getAppCode().equals(ticket.getAppCode())) throw invalidSelectorTicket();
        List<ResolvedCandidate> candidates = resolveCandidates(ticket.getAppCode(), ticket.getMobileHash());
        if (!candidateDigest(candidates).equals(ticket.getCandidateDigest())) throw invalidSelectorTicket();
        /** 前端 bigint 字符串在服务层按 PostgreSQL bigint 正数范围安全还原。 */
        long selectedAccountId = parseAccountId(request.getAccountId());
        ResolvedCandidate selected = candidates.stream()
                .filter(candidate -> candidate.identity().accountId() == selectedAccountId)
                .findFirst()
                .orElseThrow(this::invalidSelectorTicket);
        ticketPersistenceService.consume(ticket);
        return LoginResultVO.authenticated(createSession(
                selected.identity(), request.isRememberMe(), request.getDevice(), loginIp,
                request.getAppCode()
        ));
    }

    /** @return Auth 绑定结果与 IAM 状态二次校验后的候选账号 */
    private List<ResolvedCandidate> resolveCandidates(String appCode, String mobileHash) {
        List<Long> boundAccountIds = mobileBindingMapper.selectVerifiedTenantAccountIdsByMobileHash(mobileHash);
        if (boundAccountIds.isEmpty()) return List.of();
        List<MobileAccountLoginCandidateInternalResponse> identities = iamClient.resolveMobileAccounts(
                new MobileAccountLoginResolutionInternalRequest(appCode, boundAccountIds)
        );
        if (identities.isEmpty()) return List.of();
        List<Long> resolvedAccountIds = identities.stream()
                .map(MobileAccountLoginCandidateInternalResponse::accountId)
                .toList();
        Map<Long, OffsetDateTime> latestLogins = new HashMap<>();
        for (DeviceSessionMapper.LatestLoginRow row : deviceSessionMapper.selectLatestTenantLogins(resolvedAccountIds)) {
            latestLogins.put(row.accountId(), row.latestLoginTime());
        }
        List<ResolvedCandidate> candidates = new ArrayList<>(identities.size());
        for (MobileAccountLoginCandidateInternalResponse identity : identities) {
            candidates.add(new ResolvedCandidate(identity, latestLogins.get(identity.accountId())));
        }
        candidates.sort(
                Comparator.comparing(
                                ResolvedCandidate::latestLoginTime,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(candidate -> candidate.identity().username(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(candidate -> candidate.identity().accountId())
        );
        return List.copyOf(candidates);
    }

    /** @return 新签发且只保存摘要的账号选择原始票据 */
    private String issueSelectorTicket(
            String mobileHash,
            String appCode,
            List<ResolvedCandidate> candidates
    ) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawTicket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        AccountSelectorTicketEntity entity = new AccountSelectorTicketEntity();
        entity.setTicketHash(hash(rawTicket));
        entity.setMobileHash(mobileHash);
        entity.setCandidateDigest(candidateDigest(candidates));
        entity.setAppCode(appCode);
        entity.setExpiresAt(OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).plusSeconds(
                SELECTOR_TICKET_TTL_SECONDS
        ));
        ticketPersistenceService.create(entity);
        return rawTicket;
    }

    /** @return 与顺序无关的候选账号 ID 集合摘要 */
    private String candidateDigest(List<ResolvedCandidate> candidates) {
        String canonicalIds = candidates.stream()
                .map(candidate -> candidate.identity().accountId())
                .sorted()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return hash(canonicalIds);
    }

    /** @return 不含会话策略等内部字段的前端候选投影 */
    private MobileLoginCandidateVO externalCandidate(ResolvedCandidate candidate) {
        MobileAccountLoginCandidateInternalResponse identity = candidate.identity();
        return new MobileLoginCandidateVO(
                Long.toString(identity.accountId()), identity.tenantName(), identity.organizationName(),
                identity.username(), identity.displayName(), candidate.latestLoginTime()
        );
    }

    /** @return 按租户安全策略创建的短信登录会话 */
    private com.canteen.smile.modules.auth.vo.SessionVO createSession(
            MobileAccountLoginCandidateInternalResponse identity,
            boolean requestedRememberMe,
            DeviceRequest device,
            String loginIp,
            String appCode
    ) {
        boolean rememberMe = requestedRememberMe && identity.rememberMeEnabled();
        int idleSeconds = rememberMe ? identity.rememberIdleSeconds() : identity.idleSeconds();
        int absoluteSeconds = rememberMe ? identity.rememberAbsoluteSeconds() : identity.absoluteSeconds();
        TenantSessionContext context = new TenantSessionContext(
                identity.accountId(), identity.tenantId(), identity.organizationId(),
                identity.username(), identity.displayName(), appCode, rememberMe,
                device.getDeviceId(), device.getDeviceType(), device.getDeviceName(),
                identity.authzVersion(), identity.concurrentLoginEnabled(), identity.maxDevices(),
                idleSeconds, absoluteSeconds
        );
        return tenantSessionService.createSmsSession(context, loginIp);
    }

    /** @param appCode 应用入口编码 */
    private void requireTenantApp(String appCode) {
        if (!AuthConstants.TENANT_ADMIN_APP.equals(appCode)
                && !AuthConstants.TENANT_PORTAL_APP.equals(appCode)) {
            throw new BusinessException("AUTH_1008", "当前应用入口不匹配", 403);
        }
    }

    /** @return SHA-256 小写十六进制摘要 */
    private String hash(String value) {
        return HmacRequestSigner.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /** @param accountId 前端 bigint 字符串 @return 正数账号 ID */
    private long parseAccountId(String accountId) {
        try {
            /** PostgreSQL bigint 对应的 Java long 值。 */
            long value = Long.parseLong(accountId);
            if (value <= 0L) throw invalidSelectorTicket();
            return value;
        } catch (NumberFormatException exception) {
            throw invalidSelectorTicket();
        }
    }

    /** @return 手机号没有可登录账号的统一异常 */
    private BusinessException noAvailableAccount() {
        return new BusinessException(NO_AVAILABLE_ACCOUNT_CODE, "该手机号没有当前入口可用的账号", 404);
    }

    /** @return 不泄露票据内部状态的统一异常 */
    private BusinessException invalidSelectorTicket() {
        return new BusinessException(INVALID_SELECTOR_TICKET_CODE, "账号选择凭证无效或已过期", 400);
    }

    /** @param identity IAM 账号快照 @param latestLoginTime Auth 最近登录时间 */
    private record ResolvedCandidate(
            MobileAccountLoginCandidateInternalResponse identity,
            OffsetDateTime latestLoginTime
    ) {
    }
}
