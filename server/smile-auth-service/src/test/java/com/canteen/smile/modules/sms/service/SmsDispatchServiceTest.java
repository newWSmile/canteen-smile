package com.canteen.smile.modules.sms.service;

import com.canteen.smile.config.SmsProperties;
import com.canteen.smile.modules.sms.client.SmsClient;
import com.canteen.smile.modules.sms.client.SmsSendCommand;
import com.canteen.smile.modules.sms.client.SmsSendResult;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import com.canteen.smile.modules.sms.model.SmsRuntimePolicy;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 短信统一发送编排测试。 */
class SmsDispatchServiceTest {

    /** 验证策略收到真实参数，但数据库只接收脱敏手机号和安全正文。 */
    @Test
    void shouldDispatchRawCommandAndPersistSafeSnapshot() {
        SmsProperties properties = new SmsProperties();
        properties.setProviderCode("LOCAL_DATABASE_LOG");
        properties.setMobileHashPepper("local-test-pepper-with-sufficient-entropy");
        SmsClientRouter router = mock(SmsClientRouter.class);
        SmsDeliveryRecordService recordService = mock(SmsDeliveryRecordService.class);
        SmsRuntimePolicyService policyService = mock(SmsRuntimePolicyService.class);
        when(policyService.current()).thenReturn(new SmsRuntimePolicy(
                300, 60, 5, 5, 10, 30, 100, 10, 30, false, null, 0
        ));
        SmsClient client = mock(SmsClient.class);
        SmsDispatchService service = new SmsDispatchService(
                properties,
                router,
                new SmsContentSanitizer(),
                new MobileProtectionService(properties),
                recordService,
                policyService
        );
        SmsSendCommand command = new SmsSendCommand(
                "request-1",
                "challenge-1",
                SmsPurpose.LOGIN,
                "13800138000",
                null,
                "手机号 13800138000 的验证码是 482931，5 分钟内有效。",
                Map.of("code", "482931"),
                Set.of("482931")
        );
        SmsSendResult result = new SmsSendResult("request-1", OffsetDateTime.now());
        when(router.resolve("LOCAL_DATABASE_LOG")).thenReturn(client);
        when(client.providerCode()).thenReturn("LOCAL_DATABASE_LOG");
        when(client.send(command)).thenReturn(result);

        service.dispatch(command);

        verify(recordService).claim(
                eq("LOCAL_DATABASE_LOG"),
                eq(command),
                eq("手机号 138****8000 的验证码是 ******，5 分钟内有效。"),
                argThat(mobile -> "138****8000".equals(mobile.masked()) && mobile.hash().length() == 64),
                eq(false)
        );
        verify(client).send(command);
        verify(recordService).recordAccepted("request-1", result);
    }
}
