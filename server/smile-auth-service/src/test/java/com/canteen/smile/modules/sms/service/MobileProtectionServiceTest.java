package com.canteen.smile.modules.sms.service;

import com.canteen.smile.config.SmsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 手机号 HMAC 查询摘要和展示脱敏测试。 */
class MobileProtectionServiceTest {

    /** 验证相同手机号产生稳定摘要且不泄露手机号原文。 */
    @Test
    void shouldCreateStableHashAndMaskedMobile() {
        SmsProperties properties = new SmsProperties();
        properties.setMobileHashPepper("local-test-pepper-with-sufficient-entropy");
        MobileProtectionService service = new MobileProtectionService(properties);

        MobileProtectionService.ProtectedMobile first = service.protect("13800138000");
        MobileProtectionService.ProtectedMobile second = service.protect(" 13800138000 ");

        assertThat(first.hash()).hasSize(64).isEqualTo(second.hash());
        assertThat(first.hash()).doesNotContain("13800138000");
        assertThat(first.masked()).isEqualTo("138****8000");
    }
}
