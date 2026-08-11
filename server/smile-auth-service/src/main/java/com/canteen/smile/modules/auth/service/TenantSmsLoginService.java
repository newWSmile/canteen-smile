package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.dto.MobileAccountLoginCandidateInternalResponse;
import com.canteen.smile.modules.auth.dto.AccountSelectionLoginRequest;
import com.canteen.smile.modules.auth.dto.DeviceRequest;
import com.canteen.smile.modules.auth.dto.SmsLoginRequest;
import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import com.canteen.smile.modules.auth.model.AccountSelectorFlow;
import com.canteen.smile.modules.auth.vo.LoginResultVO;
import com.canteen.smile.modules.auth.service.TenantMobileAccountResolver.ResolvedCandidate;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.service.SmsChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** 手机号验证码登录、候选账号选择和设备会话建立服务。 */
@Service
@RequiredArgsConstructor
public class TenantSmsLoginService {

    /** 手机号没有当前入口可登录账号时使用的稳定错误码。 */
    private static final String NO_AVAILABLE_ACCOUNT_CODE = "AUTH_1015";

    /** 账号选择票据有效秒数。 */
    private static final long SELECTOR_TICKET_TTL_SECONDS = 300L;

    /** 手机号验证码挑战服务。 */
    private final SmsChallengeService smsChallengeService;

    /** 已验证手机号的租户账号候选解析服务。 */
    private final TenantMobileAccountResolver accountResolver;

    /** 账号选择票据事务服务。 */
    private final AccountSelectorTicketPersistenceService ticketPersistenceService;

    /** 租户设备会话服务。 */
    private final TenantSessionService tenantSessionService;

    /**
     * 验证 LOGIN 用途短信验证码；单账号直接登录，多账号返回短期选择票据。
     *
     * @param request 短信验证码登录请求
     * @param loginIp 服务端取得的来源 IP
     * @return 已认证会话或账号选择结果
     */
    public LoginResultVO login(SmsLoginRequest request, String loginIp) {
        accountResolver.requireTenantApp(request.getAppCode());
        SmsChallengeVerificationResult verification = smsChallengeService.verifyAndConsume(
                request.getChallengeId(), request.getCode(), SmsPurpose.LOGIN
        );
        List<ResolvedCandidate> candidates = accountResolver.resolve(
                request.getAppCode(), verification.mobileHash()
        );
        if (candidates.isEmpty()) throw noAvailableAccount();
        if (candidates.size() == 1) {
            return LoginResultVO.authenticated(createSession(
                    candidates.get(0).identity(), request.isRememberMe(), request.getDevice(), loginIp,
                    request.getAppCode()
            ));
        }
        String rawTicket = ticketPersistenceService.issue(
                verification.mobileHash(),
                accountResolver.candidateDigest(candidates),
                request.getAppCode(),
                AccountSelectorFlow.LOGIN,
                OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).plusSeconds(
                        SELECTOR_TICKET_TTL_SECONDS
                )
        );
        return LoginResultVO.accountSelectionRequired(
                rawTicket,
                candidates.stream().map(accountResolver::externalCandidate).toList()
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
        accountResolver.requireTenantApp(request.getAppCode());
        AccountSelectorTicketEntity ticket = ticketPersistenceService.requireActive(
                request.getAccountSelectorTicket(), AccountSelectorFlow.LOGIN
        );
        if (!request.getAppCode().equals(ticket.getAppCode())) {
            throw accountResolver.invalidSelectorTicket();
        }
        List<ResolvedCandidate> candidates = accountResolver.resolve(
                ticket.getAppCode(), ticket.getMobileHash()
        );
        if (!accountResolver.candidateDigest(candidates).equals(ticket.getCandidateDigest())) {
            throw accountResolver.invalidSelectorTicket();
        }
        /** 前端 bigint 字符串在服务层按 PostgreSQL bigint 正数范围安全还原。 */
        long selectedAccountId = accountResolver.parseAccountId(request.getAccountId());
        ResolvedCandidate selected = candidates.stream()
                .filter(candidate -> candidate.identity().accountId() == selectedAccountId)
                .findFirst()
                .orElseThrow(accountResolver::invalidSelectorTicket);
        ticketPersistenceService.consume(ticket);
        return LoginResultVO.authenticated(createSession(
                selected.identity(), request.isRememberMe(), request.getDevice(), loginIp,
                request.getAppCode()
        ));
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

    /** @return 手机号没有可登录账号的统一异常 */
    private BusinessException noAvailableAccount() {
        return new BusinessException(NO_AVAILABLE_ACCOUNT_CODE, "该手机号没有当前入口可用的账号", 404);
    }

}
