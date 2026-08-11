package com.canteen.smile.modules.sms.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.sms.model.SmsRuntimePolicy;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.infrastructure.redis.RedisKeyBuilder;
import com.canteen.smile.infrastructure.redis.RedisKeyProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/** 短信 Redis Cluster 多维限流测试。 */
class SmsChallengeRateLimitServiceTest {

    /** 验证七个 Key 位于同一 Cluster 哈希槽且不包含 IP 和设备原文。 */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldAcquireAllRateLimitsInOneClusterSlot() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(0L);
        SmsChallengeRateLimitService service = service(redisTemplate);

        service.acquire(
                "mobile-hash", "192.168.0.10", "device-secret", SmsPurpose.LOGIN, policy()
        );

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any(Object[].class));
        assertThat(keysCaptor.getValue()).hasSize(7).allMatch(key -> key.contains("{sms-rate-limit}"));
        assertThat(keysCaptor.getValue()).noneMatch(key -> key.contains("192.168.0.10"));
        assertThat(keysCaptor.getValue()).noneMatch(key -> key.contains("device-secret"));
        assertThat(keysCaptor.getValue().subList(0, 3))
                .allMatch(key -> key.contains("login"));
    }

    /** 验证六十秒重发锁返回统一稳定限流错误码。 */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldRejectRequestWithinResendInterval() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
        SmsChallengeRateLimitService service = service(redisTemplate);

        assertThatThrownBy(() -> service.acquire(
                "mobile-hash", "127.0.0.1", "device-1", SmsPurpose.LOGIN, policy()
        ))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("AUTH_1005");
                    assertThat(exception.getHttpStatus()).isEqualTo(429);
                });
    }

    /** 验证登录和绑定拥有独立手机号窗口，同时仍共享 IP 与设备总控窗口。 */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldSeparatePurposeMobileWindowsAndKeepSecurityWindowsGlobal() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(0L);
        SmsChallengeRateLimitService service = service(redisTemplate);

        service.acquire("mobile-hash", "127.0.0.1", "device-1", SmsPurpose.LOGIN, policy());
        service.acquire("mobile-hash", "127.0.0.1", "device-1", SmsPurpose.MOBILE_BIND, policy());

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate, times(2)).execute(
                any(RedisScript.class), keysCaptor.capture(), any(Object[].class)
        );
        List<String> loginKeys = keysCaptor.getAllValues().get(0);
        List<String> bindingKeys = keysCaptor.getAllValues().get(1);
        assertThat(loginKeys.subList(0, 3)).isNotEqualTo(bindingKeys.subList(0, 3));
        assertThat(loginKeys.subList(3, 7)).isEqualTo(bindingKeys.subList(3, 7));
    }

    /** @param redisTemplate Redis 客户端替身 @return 使用默认安全阈值的限流服务 */
    private SmsChallengeRateLimitService service(StringRedisTemplate redisTemplate) {
        RedisKeyBuilder keyBuilder = new RedisKeyBuilder(new RedisKeyProperties("canteen-smile-test"));
        return new SmsChallengeRateLimitService(redisTemplate, keyBuilder);
    }

    /** @return 默认限流策略 */
    private SmsRuntimePolicy policy() {
        return new SmsRuntimePolicy(300, 60, 5, 5, 10, 30, 100, 10, 30, false, null, 0);
    }
}
