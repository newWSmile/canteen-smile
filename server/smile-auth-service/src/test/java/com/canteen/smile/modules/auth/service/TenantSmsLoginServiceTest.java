package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.IamPlatformIdentityClient;
import com.canteen.smile.internal.client.dto.MobileAccountLoginCandidateInternalResponse;
import com.canteen.smile.modules.auth.dto.AccountSelectionLoginRequest;
import com.canteen.smile.modules.auth.dto.DeviceRequest;
import com.canteen.smile.modules.auth.dto.SmsLoginRequest;
import com.canteen.smile.modules.auth.entity.AccountSelectorTicketEntity;
import com.canteen.smile.modules.auth.mapper.DeviceSessionMapper;
import com.canteen.smile.modules.auth.mapper.MobileBindingMapper;
import com.canteen.smile.modules.auth.vo.SessionVO;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.service.SmsChallengeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    /** 手机号绑定数据访问替身。 */
    private MobileBindingMapper mobileBindingMapper;

    /** IAM Client 替身。 */
    private IamPlatformIdentityClient iamClient;

    /** 会话数据访问替身。 */
    private DeviceSessionMapper deviceSessionMapper;

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
        mobileBindingMapper = mock(MobileBindingMapper.class);
        iamClient = mock(IamPlatformIdentityClient.class);
        deviceSessionMapper = mock(DeviceSessionMapper.class);
        ticketPersistenceService = mock(AccountSelectorTicketPersistenceService.class);
        tenantSessionService = mock(TenantSessionService.class);
        service = new TenantSmsLoginService(
                smsChallengeService, mobileBindingMapper, iamClient, deviceSessionMapper,
                ticketPersistenceService, tenantSessionService
        );
    }

    /** 验证手机号只绑定一个有效账号时直接创建短信设备会话。 */
    @Test
    void shouldCreateSessionDirectlyForSingleAccount() {
        SmsLoginRequest request = smsRequest();
        when(smsChallengeService.verifyAndConsume("challenge-1", "482931", SmsPurpose.LOGIN))
                .thenReturn(new SmsChallengeVerificationResult("challenge-1", SmsPurpose.LOGIN, "mobile-hash"));
        when(mobileBindingMapper.selectVerifiedTenantAccountIdsByMobileHash("mobile-hash"))
                .thenReturn(List.of(7L));
        when(iamClient.resolveMobileAccounts(any())).thenReturn(List.of(candidate(7L, "user_7")));
        when(deviceSessionMapper.selectLatestTenantLogins(List.of(7L))).thenReturn(List.of());
        SessionVO session = new SessionVO(
                "satoken", "token", "session", "TENANT_ADMIN", "TENANT_ACCOUNT",
                "7", "2", "3", OffsetDateTime.now().plusHours(2), OffsetDateTime.now().plusDays(7)
        );
        when(tenantSessionService.createSmsSession(any(), eq("127.0.0.1"))).thenReturn(session);

        var result = service.login(request, "127.0.0.1");

        assertThat(result.nextStep()).isEqualTo("AUTHENTICATED");
        assertThat(result.session()).isSameAs(session);
        verify(ticketPersistenceService, never()).create(any());
    }

    /** 验证多账号时只持久化摘要并返回最近登录优先的安全候选。 */
    @Test
    void shouldIssueSelectorTicketForMultipleAccounts() {
        SmsLoginRequest request = smsRequest();
        when(smsChallengeService.verifyAndConsume("challenge-1", "482931", SmsPurpose.LOGIN))
                .thenReturn(new SmsChallengeVerificationResult("challenge-1", SmsPurpose.LOGIN, "mobile-hash"));
        when(mobileBindingMapper.selectVerifiedTenantAccountIdsByMobileHash("mobile-hash"))
                .thenReturn(List.of(7L, 8L));
        when(iamClient.resolveMobileAccounts(any())).thenReturn(List.of(
                candidate(7L, "user_7"), candidate(8L, "user_8")
        ));
        when(deviceSessionMapper.selectLatestTenantLogins(List.of(7L, 8L))).thenReturn(List.of(
                new DeviceSessionMapper.LatestLoginRow(8L, OffsetDateTime.now())
        ));

        var result = service.login(request, "127.0.0.1");

        assertThat(result.nextStep()).isEqualTo("ACCOUNT_SELECTION_REQUIRED");
        assertThat(result.accountSelectorTicket()).isNotBlank();
        assertThat(result.accountCandidates()).extracting(candidate -> candidate.accountId())
                .containsExactly("8", "7");
        ArgumentCaptor<AccountSelectorTicketEntity> ticketCaptor =
                ArgumentCaptor.forClass(AccountSelectorTicketEntity.class);
        verify(ticketPersistenceService).create(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getMobileHash()).isEqualTo("mobile-hash");
        assertThat(ticketCaptor.getValue().getCandidateDigest()).hasSize(64);
    }

    /** 验证选择不属于候选集合的账号时票据不会被消费。 */
    @Test
    void shouldRejectAccountOutsideCandidateSet() {
        AccountSelectorTicketEntity ticket = new AccountSelectorTicketEntity();
        ticket.setMobileHash("mobile-hash");
        ticket.setAppCode("TENANT_ADMIN");
        ticket.setCandidateDigest("invalid-for-current-set");
        when(ticketPersistenceService.requireActive(any())).thenReturn(ticket);
        when(mobileBindingMapper.selectVerifiedTenantAccountIdsByMobileHash("mobile-hash"))
                .thenReturn(List.of(7L));
        when(iamClient.resolveMobileAccounts(any())).thenReturn(List.of(candidate(7L, "user_7")));
        when(deviceSessionMapper.selectLatestTenantLogins(List.of(7L))).thenReturn(List.of());
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
}
