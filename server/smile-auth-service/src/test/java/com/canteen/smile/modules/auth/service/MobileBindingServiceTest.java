package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.config.MobileEncryptionProperties;
import com.canteen.smile.config.SmsProperties;
import com.canteen.smile.modules.auth.dto.MobileBindingConfirmRequest;
import com.canteen.smile.modules.auth.entity.MobileBindingEntity;
import com.canteen.smile.modules.sms.model.SmsChallengeVerificationResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.service.MobileProtectionService;
import com.canteen.smile.modules.sms.service.SmsChallengeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 当前租户账号手机号首次绑定业务测试。 */
class MobileBindingServiceTest {

    /** 当前可信主体服务替身。 */
    private CurrentAuthSubjectService subjectService;

    /** 手机号绑定持久化服务替身。 */
    private MobileBindingPersistenceService persistenceService;

    /** 短信挑战服务替身。 */
    private SmsChallengeService challengeService;

    /** 手机号安全投影服务。 */
    private MobileProtectionService protectionService;

    /** 被测试绑定服务。 */
    private MobileBindingService bindingService;

    /** 初始化独立安全配置和当前租户账号。 */
    @BeforeEach
    void setUp() {
        subjectService = mock(CurrentAuthSubjectService.class);
        persistenceService = mock(MobileBindingPersistenceService.class);
        challengeService = mock(SmsChallengeService.class);
        SmsProperties smsProperties = new SmsProperties();
        smsProperties.setMobileHashPepper("mobile-binding-test-pepper-with-sufficient-entropy");
        protectionService = new MobileProtectionService(smsProperties);
        MobileEncryptionProperties encryptionProperties = new MobileEncryptionProperties();
        encryptionProperties.setKeyId("test-v1");
        encryptionProperties.setKey(Base64.getEncoder().encodeToString(new byte[32]));
        bindingService = new MobileBindingService(
                subjectService,
                persistenceService,
                challengeService,
                protectionService,
                new MobileCipherService(encryptionProperties)
        );
        when(subjectService.currentTenant()).thenReturn(
                new CurrentAuthSubjectService.CurrentTenantSubject(7L, 2L, "test_user", "测试用户")
        );
    }

    /** 验证正确挑战只为当前账号保存摘要、脱敏值和认证密文。 */
    @Test
    void shouldBindVerifiedMobileToCurrentAccount() {
        String mobileHash = protectionService.hashForSearch("13800138000");
        when(challengeService.verifyAndConsume("challenge-1", "482931", SmsPurpose.MOBILE_BIND))
                .thenReturn(new SmsChallengeVerificationResult(
                        "challenge-1", SmsPurpose.MOBILE_BIND, mobileHash
                ));

        var result = bindingService.confirm(
                new MobileBindingConfirmRequest("13800138000", "challenge-1", "482931")
        );

        ArgumentCaptor<MobileBindingEntity> entityCaptor = ArgumentCaptor.forClass(MobileBindingEntity.class);
        verify(persistenceService).bind(
                entityCaptor.capture(),
                eq(new CurrentAuthSubjectService.CurrentTenantSubject(7L, 2L, "test_user", "测试用户"))
        );
        MobileBindingEntity entity = entityCaptor.getValue();
        assertThat(result.bound()).isTrue();
        assertThat(result.maskedMobile()).isEqualTo("138****8000");
        assertThat(entity.getSubjectId()).isEqualTo(7L);
        assertThat(entity.getMobileHash()).isEqualTo(mobileHash);
        assertThat(entity.getEncryptionKeyId()).isEqualTo("test-v1");
        assertThat(new String(entity.getMobileCiphertext(), StandardCharsets.UTF_8))
                .doesNotContain("13800138000");
    }

    /** 验证提交手机号与已验证挑战不匹配时不会写入绑定。 */
    @Test
    void shouldRejectDifferentMobileFromVerifiedChallenge() {
        when(challengeService.verifyAndConsume("challenge-2", "482931", SmsPurpose.MOBILE_BIND))
                .thenReturn(new SmsChallengeVerificationResult(
                        "challenge-2",
                        SmsPurpose.MOBILE_BIND,
                        protectionService.hashForSearch("13900139000")
                ));

        assertThatThrownBy(() -> bindingService.confirm(
                new MobileBindingConfirmRequest("13800138000", "challenge-2", "482931")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码无效、已过期或已使用");
        verify(persistenceService, never()).bind(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
