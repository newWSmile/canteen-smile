package com.canteen.smile.modules.sms.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.config.SmsProperties;
import com.canteen.smile.modules.sms.client.SmsSendCommand;
import com.canteen.smile.modules.sms.client.SmsClient;
import com.canteen.smile.modules.sms.client.SmsClientConfiguration;
import com.canteen.smile.modules.sms.dto.SmsChallengeCreateRequest;
import com.canteen.smile.modules.sms.entity.SmsChallengeEntity;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.model.SmsRuntimePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 短信挑战创建、发送和一次性校验测试。 */
class SmsChallengeServiceTest {

    /** 测试短信配置。 */
    private SmsProperties properties;

    /** 挑战持久化服务替身。 */
    private SmsChallengePersistenceService persistenceService;

    /** 限流服务替身。 */
    private SmsChallengeRateLimitService rateLimitService;

    /** 短信分发服务替身。 */
    private SmsDispatchService dispatchService;

    /** 短信策略路由替身。 */
    private SmsClientRouter smsClientRouter;

    /** 验证码安全服务。 */
    private SmsChallengeCodeService codeService;

    /** 被测试短信挑战服务。 */
    private SmsChallengeService challengeService;

    /** 数据库短信策略替身。 */
    private SmsRuntimePolicyService policyService;

    /** 初始化五分钟、五次错误限制的隔离测试对象。 */
    @BeforeEach
    void setUp() {
        properties = new SmsProperties();
        properties.setMobileHashPepper("local-test-mobile-pepper-with-sufficient-entropy");
        properties.setCodeHashPepper("local-test-code-pepper-with-sufficient-entropy");
        persistenceService = mock(SmsChallengePersistenceService.class);
        rateLimitService = mock(SmsChallengeRateLimitService.class);
        dispatchService = mock(SmsDispatchService.class);
        smsClientRouter = mock(SmsClientRouter.class);
        SmsClient smsClient = mock(SmsClient.class);
        when(smsClientRouter.resolve("LOCAL_DATABASE_LOG")).thenReturn(smsClient);
        when(smsClient.configurationFor(SmsPurpose.LOGIN))
                .thenReturn(SmsClientConfiguration.localUnmanaged());
        codeService = new SmsChallengeCodeService(properties);
        policyService = mock(SmsRuntimePolicyService.class);
        when(policyService.current()).thenReturn(policy());
        challengeService = new SmsChallengeService(
                persistenceService,
                codeService,
                rateLimitService,
                new MobileProtectionService(properties),
                dispatchService,
                smsClientRouter,
                properties,
                policyService
        );
    }

    /** 验证创建挑战时持久化验证码摘要并正式调用短信分发服务。 */
    @Test
    void shouldCreateChallengeAndDispatchSms() {
        SmsChallengeCreateRequest request = request();
        ArgumentCaptor<SmsChallengeEntity> entityCaptor = ArgumentCaptor.forClass(SmsChallengeEntity.class);
        ArgumentCaptor<SmsSendCommand> commandCaptor = ArgumentCaptor.forClass(SmsSendCommand.class);

        var response = challengeService.create(request, "127.0.0.1");

        String expectedMobileHash = new MobileProtectionService(properties).hashForSearch("13800138000");
        verify(rateLimitService).acquire(expectedMobileHash, "127.0.0.1", "device-1", policy());
        verify(persistenceService).create(entityCaptor.capture());
        verify(dispatchService).dispatch(commandCaptor.capture());
        SmsChallengeEntity entity = entityCaptor.getValue();
        SmsSendCommand command = commandCaptor.getValue();
        String code = command.templateParameters().get("code");
        assertThat(response.challengeId()).isEqualTo(entity.getChallengeId()).isEqualTo(command.challengeId());
        assertThat(response.maskedMobile()).isEqualTo("138****8000");
        assertThat(code).matches("^[0-9]{6}$");
        assertThat(entity.getCodeHash()).doesNotContain(code);
        assertThat(codeService.matches(
                entity.getCodeHash(),
                entity.getChallengeId(),
                SmsPurpose.LOGIN,
                entity.getMobileHash(),
                code
        )).isTrue();
    }

    /** 验证正确验证码只能通过数据库原子消费后返回安全手机号摘要。 */
    @Test
    void shouldVerifyAndConsumeCorrectCode() {
        SmsChallengeEntity entity = challenge("challenge-1", "482931");
        when(persistenceService.find("challenge-1")).thenReturn(entity);
        when(persistenceService.consume("challenge-1", 5)).thenReturn(true);

        SmsChallengeVerificationResult result = challengeService.verifyAndConsume(
                "challenge-1",
                "482931",
                SmsPurpose.LOGIN
        );

        assertThat(result.mobileHash()).isEqualTo(entity.getMobileHash());
        verify(persistenceService).consume("challenge-1", 5);
    }

    /** 验证错误验证码累计失败次数且不泄露挑战状态。 */
    @Test
    void shouldRecordIncorrectCodeAttempt() {
        SmsChallengeEntity entity = challenge("challenge-2", "482931");
        when(persistenceService.find("challenge-2")).thenReturn(entity);

        assertThatThrownBy(() -> challengeService.verifyAndConsume(
                "challenge-2",
                "000000",
                SmsPurpose.LOGIN
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码无效、已过期或已使用");
        verify(persistenceService).recordFailure(eq("challenge-2"), eq(5));
    }

    /** @return 标准登录验证码创建请求 */
    private SmsChallengeCreateRequest request() {
        SmsChallengeCreateRequest request = new SmsChallengeCreateRequest();
        request.setPurpose(SmsPurpose.LOGIN);
        request.setMobile("13800138000");
        request.setDeviceId("device-1");
        return request;
    }

    /** @param challengeId 挑战标识 @param code 正确验证码 @return 未过期挑战 */
    private SmsChallengeEntity challenge(String challengeId, String code) {
        String mobileHash = new MobileProtectionService(properties).hashForSearch("13800138000");
        SmsChallengeEntity entity = new SmsChallengeEntity();
        entity.setChallengeId(challengeId);
        entity.setPurpose(SmsPurpose.LOGIN.name());
        entity.setMobileHash(mobileHash);
        entity.setCodeHash(codeService.hash(challengeId, SmsPurpose.LOGIN, mobileHash, code));
        entity.setAttempts(0);
        entity.setStatus("PENDING");
        entity.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
        return entity;
    }

    /** @return 测试固定短信运行策略 */
    private SmsRuntimePolicy policy() {
        return new SmsRuntimePolicy(300, 60, 5, 5, 10, 30, 100, 10, 30, false, null, 0);
    }
}
