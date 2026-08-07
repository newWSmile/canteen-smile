package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.config.PasswordEnvelopeProperties;
import com.canteen.smile.infrastructure.redis.RedisKeyBuilder;
import com.canteen.smile.infrastructure.redis.RedisKeyProperties;
import com.canteen.smile.modules.auth.dto.PasswordEnvelopeRequest;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.vo.PasswordEncryptionChallengeVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 密码混合加密信封的密码学兼容性和一次性消费测试。 */
class PasswordEnvelopeServiceTest {

    /** 模拟 Redis 中当前尚未消费的挑战 JSON。 */
    private final AtomicReference<String> storedChallenge = new AtomicReference<>();

    /** 被测试的一次性密码信封服务。 */
    private PasswordEnvelopeService service;

    /** 为每个测试创建独立的内存 Redis 行为。 */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        /** 模拟 Redis 字符串客户端。 */
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        /** 模拟 Redis 字符串值操作。 */
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> {
            storedChallenge.set(invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(valueOperations.getAndDelete(anyString()))
                .thenAnswer(invocation -> storedChallenge.getAndSet(null));

        /** 测试使用的安全范围密码信封配置。 */
        PasswordEnvelopeProperties properties = new PasswordEnvelopeProperties();
        properties.setChallengeTtlSeconds(120);
        properties.setRsaKeySize(2048);
        properties.setRsaKeyRotationDays(30);
        service = new PasswordEnvelopeService(
                redisTemplate,
                new RedisKeyBuilder(new RedisKeyProperties("canteen-smile-test")),
                new ObjectMapper(),
                properties
        );
    }

    /** 验证 RSA-OAEP/AES-GCM 可以解密一次，重放同一挑战会被拒绝。 */
    @Test
    void shouldDecryptEnvelopeOnlyOnce() throws Exception {
        /** Auth 签发的短期公钥挑战。 */
        PasswordEncryptionChallengeVO challenge = service.issue(
                PasswordEnvelopePurpose.PLATFORM_PASSWORD_LOGIN
        );
        /** 使用与浏览器相同算法生成的测试密码信封。 */
        PasswordEnvelopeRequest envelope = encrypt("River!Stone2026", challenge);

        assertThat(service.decrypt(envelope, PasswordEnvelopePurpose.PLATFORM_PASSWORD_LOGIN))
                .isEqualTo("River!Stone2026");
        assertThatThrownBy(() -> service.decrypt(
                envelope,
                PasswordEnvelopePurpose.PLATFORM_PASSWORD_LOGIN
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo("AUTH_1014")
        );
    }

    /** 验证同一轮换周期内复用 RSA 密钥，但每次挑战仍具有独立 nonce。 */
    @Test
    void shouldReuseRsaKeyDuringRotationPeriod() {
        /** 第一次签发的密码挑战。 */
        PasswordEncryptionChallengeVO firstChallenge = service.issue(
                PasswordEnvelopePurpose.PLATFORM_PASSWORD_LOGIN
        );
        /** 同一轮换周期内第二次签发的密码挑战。 */
        PasswordEncryptionChallengeVO secondChallenge = service.issue(
                PasswordEnvelopePurpose.PLATFORM_PASSWORD_LOGIN
        );

        assertThat(secondChallenge.keyId()).isEqualTo(firstChallenge.keyId());
        assertThat(secondChallenge.publicKey()).isEqualTo(firstChallenge.publicKey());
        assertThat(secondChallenge.nonce()).isNotEqualTo(firstChallenge.nonce());
        assertThat(secondChallenge.timestamp()).isGreaterThanOrEqualTo(firstChallenge.timestamp());
    }

    /**
     * 使用 Java 模拟浏览器 Web Crypto 的 RSA-OAEP/AES-GCM 信封生成过程。
     *
     * @param password 测试密码
     * @param challenge 服务端短期公钥挑战
     * @return 可交给 Auth 解密的密码信封
     */
    private PasswordEnvelopeRequest encrypt(
            String password,
            PasswordEncryptionChallengeVO challenge
    ) throws Exception {
        /** 挑战绑定的附加认证数据。 */
        byte[] additionalData = String.join(
                "\n",
                challenge.purpose().name(),
                challenge.keyId(),
                challenge.nonce(),
                Long.toString(challenge.timestamp())
        ).getBytes(StandardCharsets.UTF_8);
        /** 从挑战恢复的临时 RSA 公钥。 */
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(challenge.publicKey()))
        );
        /** 当前密码独占的随机 AES-256 密钥。 */
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey aesKey = keyGenerator.generateKey();
        /** 使用 RSA-OAEP-SHA256 包装后的 AES 密钥。 */
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(
                Cipher.ENCRYPT_MODE,
                publicKey,
                new OAEPParameterSpec(
                        "SHA-256",
                        "MGF1",
                        MGF1ParameterSpec.SHA256,
                        new PSource.PSpecified(additionalData)
                )
        );
        byte[] encryptedKey = rsaCipher.doFinal(aesKey.getEncoded());
        /** 当前密码独占的 96 位 GCM 初始向量。 */
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        /** 包含 128 位认证标签的 AES-GCM 密文。 */
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
        aesCipher.updateAAD(additionalData);
        byte[] ciphertext = aesCipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return new PasswordEnvelopeRequest(
                challenge.keyId(),
                challenge.nonce(),
                challenge.timestamp(),
                Base64.getEncoder().encodeToString(encryptedKey),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(ciphertext)
        );
    }
}
