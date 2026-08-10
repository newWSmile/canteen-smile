package com.canteen.smile.modules.sms.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 短信正文安全快照测试。 */
class SmsContentSanitizerTest {

    /** 验证验证码和完整手机号不会进入数据库或普通日志正文。 */
    @Test
    void shouldMaskSensitiveValueAndMobile() {
        SmsContentSanitizer sanitizer = new SmsContentSanitizer();

        String result = sanitizer.sanitize(
                "手机号 13800138000 的验证码是 482931，5 分钟内有效。",
                Set.of("482931"),
                "13800138000",
                "138****8000"
        );

        assertThat(result).isEqualTo("手机号 138****8000 的验证码是 ******，5 分钟内有效。");
        assertThat(result).doesNotContain("482931", "13800138000");
    }

    /** 验证安全开关开启时只允许保留验证码，完整手机号仍必须脱敏。 */
    @Test
    void shouldRetainCodeButAlwaysMaskMobileWhenExplicitlyEnabled() {
        SmsContentSanitizer sanitizer = new SmsContentSanitizer();

        String result = sanitizer.sanitize(
                "手机号 13800138000 的验证码是 482931，5 分钟内有效。",
                Set.of("482931"),
                "13800138000",
                "138****8000",
                true
        );

        assertThat(result).isEqualTo("手机号 138****8000 的验证码是 482931，5 分钟内有效。");
        assertThat(result).doesNotContain("13800138000");
    }
}
