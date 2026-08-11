package com.canteen.smile.modules.sms.service;

import com.canteen.smile.config.SmsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    /** 验证超长号码、非数字和不合法号段不能进入摘要与发送流程。 */
    @Test
    void shouldRejectInvalidMainlandMobile() {
        SmsProperties properties = new SmsProperties();
        properties.setMobileHashPepper("local-test-pepper-with-sufficient-entropy");
        MobileProtectionService service = new MobileProtectionService(properties);

        assertThatThrownBy(() -> service.protect("135000000000000000000000"))
                .hasMessage("请输入正确的11位手机号");
        assertThatThrownBy(() -> service.protect("12500000000"))
                .hasMessage("请输入正确的11位手机号");
        assertThatThrownBy(() -> service.protect("13800A38000"))
                .hasMessage("请输入正确的11位手机号");
    }
}
