package com.canteen.smile.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/** Argon2id 密码哈希配置测试。 */
class AuthSecurityConfigurationTest {

    /** 验证编码结果使用 Argon2id 且可以正确匹配。 */
    @Test
    void shouldEncodeAndMatchWithArgon2id() {
        /** 当前 Auth Argon2id 编码器。 */
        PasswordEncoder encoder = new AuthSecurityConfiguration().passwordEncoder();
        /** 测试密码摘要。 */
        String encoded = encoder.encode("Safe!Password2026");

        assertThat(encoded).startsWith("$argon2id$");
        assertThat(encoder.matches("Safe!Password2026", encoded)).isTrue();
        assertThat(encoder.matches("Wrong!Password2026", encoded)).isFalse();
    }
}
