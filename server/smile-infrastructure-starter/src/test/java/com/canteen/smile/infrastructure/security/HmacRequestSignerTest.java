package com.canteen.smile.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** 内部请求 HMAC v1 签名契约测试。 */
class HmacRequestSignerTest {

    /** 验证规范串、查询排序和签名结果保持稳定。 */
    @Test
    void shouldCreateStableCanonicalSignature() {
        /** 测试请求体摘要。 */
        String contentHash = HmacRequestSigner.sha256Hex("{}".getBytes(StandardCharsets.UTF_8));
        /** HMAC v1 规范请求串。 */
        String canonical = HmacRequestSigner.canonicalRequest(
                "post",
                "/internal/iam/v1/platform-identities/bootstrap",
                "z=2&a=1",
                contentHash,
                "1786000000",
                "nonce-001",
                "smile-auth-service",
                "event-001"
        );
        /** 使用固定测试密钥生成的签名。 */
        String signature = HmacRequestSigner.sign("test-secret", canonical);

        assertThat(canonical).startsWith("POST\n/internal/iam/v1/platform-identities/bootstrap\na=1&z=2\n");
        assertThat(signature).matches("^[0-9a-f]{64}$");
        assertThat(HmacRequestSigner.constantTimeEquals(signature, signature)).isTrue();
        assertThat(HmacRequestSigner.constantTimeEquals(signature, signature + "0")).isFalse();
    }
}
