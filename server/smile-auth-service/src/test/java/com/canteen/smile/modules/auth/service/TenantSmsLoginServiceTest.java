package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.dto.MobileAccountLoginCandidateInternalResponse;
import com.canteen.smile.modules.auth.dto.AccountSelectionLoginRequest;
import com.canteen.smile.modules.auth.dto.DeviceRequest;
import com.canteen.smile.modules.auth.dto.SmsLoginRequest;
import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import com.canteen.smile.modules.auth.model.AccountSelectorFlow;
import com.canteen.smile.modules.auth.service.TenantMobileAccountResolver.ResolvedCandidate;
import com.canteen.smile.modules.auth.vo.MobileLoginCandidateVO;
import com.canteen.smile.modules.auth.vo.SessionVO;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.service.SmsChallengeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 手机号验证码登录与多账号选择业务测试。 */
class TenantSmsLoginServiceTest {

    /** 短信挑战服务替身。 */
    private SmsChallengeService smsChallengeService;

    /** 租户账号候选解析服务替身。 */
    private TenantMobileAccountResolver accountResolver;

    /** 账号选择票据事务替身。 */
    private AccountSelectorTicketPersistenceService ticketPersistenceService;

    /** 租户会话服务替身。 */
    private TenantSessionService tenantSessionService;

    /** 被测试服务。 */
    private TenantSmsLoginService service;

    /** 初始化依赖替身。 */
    @BeforeEach
    void setUp() {
        smsChallengeService = mock(SmsChallengeService.class);
        accountResolver = mock(TenantMobileAccountResolver.class);
        ticketPersistenceService = mock(AccountSelectorTicketPersistenceService.class);
        tenantSessionService = mock(TenantSessionService.class);
        service = new TenantSmsLoginService(
                smsChallengeService, accountResolver, ticketPersistenceService, tenantSessionService
        );
    }

    /** 验证手机号只绑定一个有效账号时直接创建短信设备会话。 */
    @Test
    void shouldCreateSessionDirectlyForSingleAccount() {
        SmsLoginRequest request = smsRequest();
        when(smsChallengeService.verifyAndConsume("challenge-1", "482931", SmsPurpose.LOGIN))
                .thenReturn(new SmsChallengeVerificationResult("challenge-1", SmsPurpose.LOGIN, "mobile-hash"));
        ResolvedCandidate candidate = resolvedCandidate(7L, "user_7", null);
        when(accountResolver.resolve("TENANT_ADMIN", "mobile-hash"))
                .thenReturn(List.of(candidate));
        SessionVO session = new SessionVO(
                "satoken", "token", "session", "TENANT_ADMIN", "TENANT_ACCOUNT",
                "7", "2", "3", OffsetDateTime.now().plusHours(2), OffsetDateTime.now().plusDays(7)
        );
        when(tenantSessionService.createSmsSession(any(), eq("127.0.0.1"))).thenReturn(session);

        var result = service.login(request, "127.0.0.1");

        assertThat(result.nextStep()).isEqualTo("AUTHENTICATED");
        assertThat(result.session()).isSameAs(session);
        verify(ticketPersistenceService, never()).issue(any(), any(), any(), any(), any());
    }

    /** 验证多账号时只持久化摘要并返回最近登录优先的安全候选。 */
    @Test
    void shouldIssueSelectorTicketForMultipleAccounts() {
        SmsLoginRequest request = smsRequest();
        when(smsChallengeService.verifyAndConsume("challenge-1", "482931", SmsPurpose.LOGIN))
                .thenReturn(new SmsChallengeVerificationResult("challenge-1", SmsPurpose.LOGIN, "mobile-hash"));
        ResolvedCandidate first = resolvedCandidate(8L, "user_8", OffsetDateTime.now());
        ResolvedCandidate second = resolvedCandidate(7L, "user_7", null);
        List<ResolvedCandidate> candidates = List.of(first, second);
        when(accountResolver.resolve("TENANT_ADMIN", "mobile-hash")).thenReturn(candidates);
        when(accountResolver.candidateDigest(candidates)).thenReturn("a".repeat(64));
        when(accountResolver.externalCandidate(first)).thenReturn(externalCandidate(first));
        when(accountResolver.externalCandidate(second)).thenReturn(externalCandidate(second));
        when(ticketPersistenceService.issue(
                eq("mobile-hash"), eq("a".repeat(64)), eq("TENANT_ADMIN"),
                eq(AccountSelectorFlow.LOGIN), any()
        )).thenReturn("selector-ticket");

        var result = service.login(request, "127.0.0.1");

        assertThat(result.nextStep()).isEqualTo("ACCOUNT_SELECTION_REQUIRED");
        assertThat(result.accountSelectorTicket()).isNotBlank();
        assertThat(result.accountCandidates()).extracting(candidate -> candidate.accountId())
                .containsExactly("8", "7");
        verify(ticketPersistenceService).issue(
                eq("mobile-hash"), eq("a".repeat(64)), eq("TENANT_ADMIN"),
                eq(AccountSelectorFlow.LOGIN), any()
        );
    }

    /** 验证选择不属于候选集合的账号时票据不会被消费。 */
    @Test
    void shouldRejectAccountOutsideCandidateSet() {
        AccountSelectorTicketEntity ticket = new AccountSelectorTicketEntity();
        ticket.setMobileHash("mobile-hash");
        ticket.setAppCode("TENANT_ADMIN");
        ticket.setFlowType(AccountSelectorFlow.LOGIN.name());
        ticket.setCandidateDigest("invalid-for-current-set");
        when(ticketPersistenceService.requireActive("selector-ticket", AccountSelectorFlow.LOGIN))
                .thenReturn(ticket);
        List<ResolvedCandidate> candidates = List.of(resolvedCandidate(7L, "user_7", null));
        when(accountResolver.resolve("TENANT_ADMIN", "mobile-hash")).thenReturn(candidates);
        when(accountResolver.candidateDigest(candidates)).thenReturn("current-digest");
        when(accountResolver.invalidSelectorTicket()).thenReturn(
                new BusinessException("AUTH_1014", "账号选择凭证无效或已过期", 400)
        );
        AccountSelectionLoginRequest request = new AccountSelectionLoginRequest();
        request.setAppCode("TENANT_ADMIN");
        request.setAccountSelectorTicket("selector-ticket");
        request.setAccountId("99");
        request.setDevice(device());

        assertThatThrownBy(() -> service.selectAccount(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号选择凭证无效或已过期");
        verify(ticketPersistenceService, never()).consume(any());
    }

    /** @return 合法短信登录请求 */
    private SmsLoginRequest smsRequest() {
        SmsLoginRequest request = new SmsLoginRequest();
        request.setAppCode("TENANT_ADMIN");
        request.setChallengeId("challenge-1");
        request.setCode("482931");
        request.setRememberMe(true);
        request.setDevice(device());
        return request;
    }

    /** @return 测试设备描述 */
    private DeviceRequest device() {
        DeviceRequest device = new DeviceRequest();
        device.setDeviceId("device-1");
        device.setDeviceType("WEB");
        device.setDeviceName("测试浏览器");
        return device;
    }

    /** @return 可登录 IAM 候选快照 */
    private MobileAccountLoginCandidateInternalResponse candidate(long accountId, String username) {
        return new MobileAccountLoginCandidateInternalResponse(
                accountId, 2L, "测试租户", 3L, "测试机构", username, username,
                1L, true, 5, true, 7200, 604800, 604800, 2592000
        );
    }

    /** @return 账号候选解析结果 */
    private ResolvedCandidate resolvedCandidate(
            long accountId,
            String username,
            OffsetDateTime latestLoginTime
    ) {
        return new ResolvedCandidate(candidate(accountId, username), latestLoginTime);
    }

    /** @return 可返回前端的账号候选 */
    private MobileLoginCandidateVO externalCandidate(ResolvedCandidate candidate) {
        return new MobileLoginCandidateVO(
                Long.toString(candidate.identity().accountId()),
                candidate.identity().tenantName(),
                candidate.identity().organizationName(),
                candidate.identity().username(),
                candidate.identity().displayName(),
                candidate.latestLoginTime()
        );
    }
}
