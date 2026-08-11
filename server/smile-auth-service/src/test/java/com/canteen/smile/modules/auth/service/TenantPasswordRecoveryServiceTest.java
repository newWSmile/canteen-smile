package com.canteen.smile.modules.auth.service;

import com.canteen.smile.internal.client.dto.MobileAccountLoginCandidateInternalResponse;
import com.canteen.smile.modules.auth.dto.SmsPasswordResetAccountSelectionRequest;
import com.canteen.smile.modules.auth.dto.SmsPasswordResetVerificationRequest;
import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import com.canteen.smile.modules.auth.model.AccountSelectorFlow;
import com.canteen.smile.modules.auth.service.TenantMobileAccountResolver.ResolvedCandidate;
import com.canteen.smile.modules.auth.vo.MobileLoginCandidateVO;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.service.SmsChallengeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 手机号验证码自助找回密码业务测试。 */
class TenantPasswordRecoveryServiceTest {

    /** 短信挑战服务替身。 */
    private SmsChallengeService smsChallengeService;

    /** 租户账号候选解析服务替身。 */
    private TenantMobileAccountResolver accountResolver;

    /** 账号选择票据事务服务替身。 */
    private AccountSelectorTicketPersistenceService selectorTicketService;

    /** 密码重置服务替身。 */
    private TenantPasswordResetService passwordResetService;

    /** 被测试服务。 */
    private TenantPasswordRecoveryService service;

    /** 初始化依赖替身。 */
    @BeforeEach
    void setUp() {
        smsChallengeService = mock(SmsChallengeService.class);
        accountResolver = mock(TenantMobileAccountResolver.class);
        selectorTicketService = mock(AccountSelectorTicketPersistenceService.class);
        passwordResetService = mock(TenantPasswordResetService.class);
        service = new TenantPasswordRecoveryService(
                smsChallengeService,
                accountResolver,
                selectorTicketService,
                passwordResetService
        );
    }

    /** 验证单账号场景直接签发短信自助重置票据。 */
    @Test
    void shouldIssueResetTicketForSingleAccount() {
        SmsPasswordResetVerificationRequest request = verificationRequest();
        ResolvedCandidate candidate = candidate(7L, "user_7");
        when(smsChallengeService.verifyAndConsume(
                "challenge-1", "482931", SmsPurpose.PASSWORD_RESET
        )).thenReturn(new SmsChallengeVerificationResult(
                "challenge-1", SmsPurpose.PASSWORD_RESET, "mobile-hash"
        ));
        when(accountResolver.resolve("TENANT_ADMIN", "mobile-hash"))
                .thenReturn(List.of(candidate));
        when(passwordResetService.issueSmsSelfService(7L, null)).thenReturn("reset-ticket");

        var result = service.verify(request);

        assertThat(result.nextStep()).isEqualTo("RESET_PASSWORD");
        assertThat(result.passwordResetTicket()).isEqualTo("reset-ticket");
        verify(smsChallengeService).verifyAndConsume(
                "challenge-1", "482931", SmsPurpose.PASSWORD_RESET
        );
    }

    /** 验证多账号场景签发与登录流程隔离的 PASSWORD_RESET 选择票据。 */
    @Test
    void shouldIssuePasswordResetSelectorForMultipleAccounts() {
        SmsPasswordResetVerificationRequest request = verificationRequest();
        ResolvedCandidate first = candidate(7L, "user_7");
        ResolvedCandidate second = candidate(8L, "user_8");
        List<ResolvedCandidate> candidates = List.of(first, second);
        when(smsChallengeService.verifyAndConsume(
                "challenge-1", "482931", SmsPurpose.PASSWORD_RESET
        )).thenReturn(new SmsChallengeVerificationResult(
                "challenge-1", SmsPurpose.PASSWORD_RESET, "mobile-hash"
        ));
        when(accountResolver.resolve("TENANT_ADMIN", "mobile-hash")).thenReturn(candidates);
        when(accountResolver.candidateDigest(candidates)).thenReturn("b".repeat(64));
        when(accountResolver.externalCandidate(first)).thenReturn(external(first));
        when(accountResolver.externalCandidate(second)).thenReturn(external(second));
        when(selectorTicketService.issue(
                eq("mobile-hash"), eq("b".repeat(64)), eq("TENANT_ADMIN"),
                eq(AccountSelectorFlow.PASSWORD_RESET), any()
        )).thenReturn("selector-ticket");

        var result = service.verify(request);

        assertThat(result.nextStep()).isEqualTo("ACCOUNT_SELECTION_REQUIRED");
        assertThat(result.accountSelectorTicket()).isEqualTo("selector-ticket");
        assertThat(result.accountCandidates()).hasSize(2);
    }

    /** 验证多账号选择后原子绑定选择票据签发最终密码重置票据。 */
    @Test
    void shouldSelectAccountWithPasswordResetFlowTicket() {
        SmsPasswordResetAccountSelectionRequest request = new SmsPasswordResetAccountSelectionRequest();
        request.setAppCode("TENANT_ADMIN");
        request.setAccountSelectorTicket("selector-ticket");
        request.setAccountId("8");
        AccountSelectorTicketEntity selectorTicket = new AccountSelectorTicketEntity();
        selectorTicket.setId(2L);
        selectorTicket.setVersion(0L);
        selectorTicket.setAppCode("TENANT_ADMIN");
        selectorTicket.setFlowType(AccountSelectorFlow.PASSWORD_RESET.name());
        selectorTicket.setMobileHash("mobile-hash");
        selectorTicket.setCandidateDigest("c".repeat(64));
        ResolvedCandidate first = candidate(7L, "user_7");
        ResolvedCandidate second = candidate(8L, "user_8");
        List<ResolvedCandidate> candidates = List.of(first, second);
        when(selectorTicketService.requireActive(
                "selector-ticket", AccountSelectorFlow.PASSWORD_RESET
        )).thenReturn(selectorTicket);
        when(accountResolver.resolve("TENANT_ADMIN", "mobile-hash")).thenReturn(candidates);
        when(accountResolver.candidateDigest(candidates)).thenReturn("c".repeat(64));
        when(accountResolver.parseAccountId("8")).thenReturn(8L);
        when(passwordResetService.issueSmsSelfService(8L, selectorTicket))
                .thenReturn("reset-ticket");

        var result = service.selectAccount(request);

        assertThat(result.nextStep()).isEqualTo("RESET_PASSWORD");
        assertThat(result.passwordResetTicket()).isEqualTo("reset-ticket");
    }

    /** @return 合法短信自助找回验证请求 */
    private SmsPasswordResetVerificationRequest verificationRequest() {
        SmsPasswordResetVerificationRequest request = new SmsPasswordResetVerificationRequest();
        request.setAppCode("TENANT_ADMIN");
        request.setChallengeId("challenge-1");
        request.setCode("482931");
        return request;
    }

    /** @return IAM 安全候选与 Auth 登录快照 */
    private ResolvedCandidate candidate(long accountId, String username) {
        return new ResolvedCandidate(
                new MobileAccountLoginCandidateInternalResponse(
                        accountId, 2L, "测试租户", 3L, "测试机构",
                        username, username, 1L, true, 5, true,
                        7200, 604800, 604800, 2592000
                ),
                null
        );
    }

    /** @return 可安全返回前端的账号候选 */
    private MobileLoginCandidateVO external(ResolvedCandidate candidate) {
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
