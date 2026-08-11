package com.canteen.smile.modules.auth.service;

import com.canteen.smile.config.MobileEncryptionProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 手机号 AES-256-GCM 密文格式和配置校验测试。 */
class MobileCipherServiceTest {

    /** 验证相同手机号每次使用独立 IV，且密文不包含手机号明文。 */
    @Test
    void shouldEncryptMobileWithIndependentIv() {
        MobileCipherService service = new MobileCipherService(configuredProperties());

        MobileCipherService.EncryptedMobile first = service.encrypt("13800138000");
        MobileCipherService.EncryptedMobile second = service.encrypt("13800138000");

        assertThat(first.keyId()).isEqualTo("local-v1");
        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
        assertThat(new String(first.ciphertext(), StandardCharsets.UTF_8)).doesNotContain("13800138000");
        assertThat(first.ciphertext()[0]).isEqualTo((byte) 1);
        assertThat(service.decrypt(first.ciphertext(), first.keyId())).isEqualTo("13800138000");
    }

    /** 验证非 256 位密钥会在业务使用前被拒绝。 */
    @Test
    void shouldRejectInvalidKeyLength() {
        MobileEncryptionProperties properties = new MobileEncryptionProperties();
        properties.setKeyId("local-v1");
        properties.setKey(Base64.getEncoder().encodeToString(new byte[16]));

        assertThatThrownBy(() -> new MobileCipherService(properties).ensureConfigured())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    /** @return 使用隔离测试密钥的手机号加密配置 */
    private MobileEncryptionProperties configuredProperties() {
        MobileEncryptionProperties properties = new MobileEncryptionProperties();
        properties.setKeyId("local-v1");
        properties.setKey(Base64.getEncoder().encodeToString(new byte[32]));
        return properties;
    }
}
