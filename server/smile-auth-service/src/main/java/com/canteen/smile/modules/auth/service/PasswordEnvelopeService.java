package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.config.PasswordEnvelopeProperties;
import com.canteen.smile.infrastructure.redis.RedisKeyBuilder;
import com.canteen.smile.modules.auth.dto.PasswordEnvelopeRequest;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.vo.PasswordEncryptionChallengeVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/** RSA-OAEP 与 AES-GCM 混合密码信封的一次性挑战及解密服务。 */
@Service
@RequiredArgsConstructor
public class PasswordEnvelopeService {

    /** 当前服务日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(PasswordEnvelopeService.class);

    /** 密码加密挑战无效、过期或已消费错误码。 */
    private static final String INVALID_ENVELOPE_CODE = "AUTH_1014";

    /** 认证运行时依赖不可用错误码。 */
    private static final String AUTH_RUNTIME_UNAVAILABLE_CODE = "AUTH_1013";

    /** RSA 密钥包装算法标识。 */
    private static final String KEY_ALGORITHM = "RSA-OAEP-256";

    /** 密码内容加密算法标识。 */
    private static final String CONTENT_ALGORITHM = "A256GCM";

    /** AES-256 原始密钥长度。 */
    private static final int AES_KEY_BYTES = 32;

    /** AES-GCM 标准 96 位初始向量长度。 */
    private static final int GCM_IV_BYTES = 12;

    /** AES-GCM 认证标签位数。 */
    private static final int GCM_TAG_BITS = 128;

    /** 允许解密后的最大密码字符数。 */
    private static final int MAX_PASSWORD_CHARACTERS = 128;

    /** 密码学安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** RSA 密钥轮换时使用的进程内互斥对象。 */
    private final Object rsaKeyRotationMonitor = new Object();

    /** 当前 Auth 实例正在使用的 RSA 密钥材料。 */
    private volatile RotatingRsaKeyMaterial currentRsaKeyMaterial;

    /** Redis 字符串客户端。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 统一 Redis Key 构建器。 */
    private final RedisKeyBuilder redisKeyBuilder;

    /** Jackson JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** 密码信封运行参数。 */
    private final PasswordEnvelopeProperties properties;

    /**
     * Auth 启动时预生成 RSA 密钥，避免首次登录承担密钥生成耗时。
     */
    @PostConstruct
    public void initializeRsaKeyMaterial() {
        validateConfiguration();
        try {
            activeRsaKeyMaterial();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password envelope RSA key could not be initialized", exception);
        }
    }

    /**
     * 使用当前轮换公钥创建绑定具体用途且只能消费一次的短期挑战。
     *
     * @param purpose 密码使用目的
     * @return 短期公钥挑战
     */
    public PasswordEncryptionChallengeVO issue(PasswordEnvelopePurpose purpose) {
        validateConfiguration();
        try {
            /** 当前轮换周期内复用的 RSA 密钥材料。 */
            RotatingRsaKeyMaterial keyMaterial = activeRsaKeyMaterial();
            /** 当前 RSA 密钥版本标识。 */
            String keyId = keyMaterial.keyId();
            /** 256 位 URL 安全随机 nonce。 */
            byte[] nonceBytes = new byte[32];
            secureRandom.nextBytes(nonceBytes);
            /** 返回浏览器并参与附加认证数据的 nonce。 */
            String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
            /** 服务端签发时间戳，客户端必须原样回传。 */
            long timestamp = Instant.now().toEpochMilli();
            /** 挑战的绝对失效时间。 */
            long expiresAtEpochMillis = timestamp + Duration.ofSeconds(properties.getChallengeTtlSeconds()).toMillis();
            /** 仅供服务端原子消费的短期私钥上下文。 */
            PasswordEnvelopeChallengeContext context = new PasswordEnvelopeChallengeContext(
                    keyId,
                    purpose.name(),
                    nonce,
                    timestamp,
                    expiresAtEpochMillis,
                    Base64.getEncoder().encodeToString(keyMaterial.keyPair().getPrivate().getEncoded())
            );
            stringRedisTemplate.opsForValue().set(
                    challengeKey(keyId, nonce),
                    objectMapper.writeValueAsString(context),
                    Duration.ofSeconds(properties.getChallengeTtlSeconds())
            );
            return new PasswordEncryptionChallengeVO(
                    purpose,
                    keyId,
                    Base64.getEncoder().encodeToString(keyMaterial.keyPair().getPublic().getEncoded()),
                    nonce,
                    timestamp,
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(expiresAtEpochMillis), ZoneOffset.UTC),
                    KEY_ALGORITHM,
                    CONTENT_ALGORITHM
            );
        } catch (GeneralSecurityException | JsonProcessingException | DataAccessException exception) {
            log.warn("Password encryption challenge could not be issued: {}", exception.getClass().getSimpleName());
            throw runtimeUnavailable();
        }
    }

    /**
     * 原子消费挑战并解密密码；任何失败都会使当前挑战不可再次使用。
     *
     * @param envelope 浏览器生成的密码信封
     * @param expectedPurpose 当前接口要求的唯一用途
     * @return 短暂存在于当前调用链中的密码明文
     */
    public String decrypt(PasswordEnvelopeRequest envelope, PasswordEnvelopePurpose expectedPurpose) {
        /** 从 Redis 原子取出并删除的一次性挑战上下文。 */
        String contextJson;
        try {
            contextJson = stringRedisTemplate.opsForValue().getAndDelete(
                    challengeKey(envelope.keyId(), envelope.nonce())
            );
        } catch (DataAccessException exception) {
            throw runtimeUnavailable();
        }
        if (contextJson == null) {
            throw invalidEnvelope();
        }
        try {
            /** 服务端签发的挑战上下文。 */
            PasswordEnvelopeChallengeContext context = objectMapper.readValue(
                    contextJson,
                    PasswordEnvelopeChallengeContext.class
            );
            validateContext(envelope, expectedPurpose, context);
            return decryptPassword(envelope, expectedPurpose, context);
        } catch (JsonProcessingException | GeneralSecurityException | IllegalArgumentException exception) {
            if (!(exception instanceof AEADBadTagException)) {
                log.debug("Password envelope rejected: {}", exception.getClass().getSimpleName());
            }
            throw invalidEnvelope();
        }
    }

    /** 校验挑战配置处于明确且安全的范围。 */
    private void validateConfiguration() {
        if (properties.getChallengeTtlSeconds() < 30 || properties.getChallengeTtlSeconds() > 300) {
            throw new IllegalStateException("Password envelope challenge TTL must be between 30 and 300 seconds");
        }
        if (properties.getRsaKeySize() != 2048
                && properties.getRsaKeySize() != 3072
                && properties.getRsaKeySize() != 4096) {
            throw new IllegalStateException("Password envelope RSA key size must be 2048, 3072 or 4096");
        }
        if (properties.getRsaKeyRotationDays() < 1 || properties.getRsaKeyRotationDays() > 90) {
            throw new IllegalStateException("Password envelope RSA key rotation days must be between 1 and 90");
        }
    }

    /**
     * 获取当前有效 RSA 密钥；到期时仅允许一个请求完成轮换。
     *
     * @return 当前有效 RSA 密钥材料
     * @throws GeneralSecurityException RSA 密钥生成失败
     */
    private RotatingRsaKeyMaterial activeRsaKeyMaterial() throws GeneralSecurityException {
        /** 当前毫秒时间戳。 */
        long now = Instant.now().toEpochMilli();
        /** 无锁读取的当前密钥快照。 */
        RotatingRsaKeyMaterial keyMaterial = currentRsaKeyMaterial;
        if (keyMaterial != null && now < keyMaterial.expiresAtEpochMillis()) {
            return keyMaterial;
        }
        synchronized (rsaKeyRotationMonitor) {
            /** 获取锁后重新读取，避免并发请求重复生成密钥。 */
            RotatingRsaKeyMaterial lockedKeyMaterial = currentRsaKeyMaterial;
            if (lockedKeyMaterial != null && now < lockedKeyMaterial.expiresAtEpochMillis()) {
                return lockedKeyMaterial;
            }
            /** 新轮换周期使用的 RSA 密钥材料。 */
            RotatingRsaKeyMaterial generatedKeyMaterial = generateRsaKeyMaterial(now);
            currentRsaKeyMaterial = generatedKeyMaterial;
            log.info("Password envelope RSA key initialized; rotationDays={}", properties.getRsaKeyRotationDays());
            return generatedKeyMaterial;
        }
    }

    /**
     * 生成一个轮换周期使用的 RSA 密钥材料。
     *
     * @param createdAtEpochMillis 密钥生成毫秒时间戳
     * @return 新 RSA 密钥材料
     * @throws GeneralSecurityException RSA 密钥生成失败
     */
    private RotatingRsaKeyMaterial generateRsaKeyMaterial(long createdAtEpochMillis)
            throws GeneralSecurityException {
        /** RSA 密钥生成器。 */
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(properties.getRsaKeySize(), secureRandom);
        /** 当前轮换周期的 RSA 密钥对。 */
        KeyPair keyPair = generator.generateKeyPair();
        /** 当前轮换周期的非业务密钥标识。 */
        String keyId = UUID.randomUUID().toString();
        /** 当前密钥的绝对轮换时间。 */
        long expiresAtEpochMillis = createdAtEpochMillis
                + Duration.ofDays(properties.getRsaKeyRotationDays()).toMillis();
        return new RotatingRsaKeyMaterial(keyId, keyPair, expiresAtEpochMillis);
    }

    /**
     * 校验客户端回传内容与服务端一次性挑战完全一致。
     *
     * @param envelope 客户端密码信封
     * @param expectedPurpose 当前接口用途
     * @param context 服务端挑战上下文
     */
    private void validateContext(
            PasswordEnvelopeRequest envelope,
            PasswordEnvelopePurpose expectedPurpose,
            PasswordEnvelopeChallengeContext context
    ) {
        if (!constantTimeEquals(envelope.keyId(), context.keyId())
                || !constantTimeEquals(envelope.nonce(), context.nonce())
                || envelope.timestamp() != context.timestamp()
                || !expectedPurpose.name().equals(context.purpose())
                || Instant.now().toEpochMilli() > context.expiresAtEpochMillis()) {
            throw invalidEnvelope();
        }
    }

    /**
     * 先使用 RSA-OAEP 解包 AES 密钥，再使用 AES-GCM 解密密码正文。
     *
     * @param envelope 客户端密码信封
     * @param purpose 当前接口用途
     * @param context 服务端挑战上下文
     * @return 已通过 UTF-8 和长度校验的密码
     * @throws GeneralSecurityException 密码学认证或解密失败
     */
    private String decryptPassword(
            PasswordEnvelopeRequest envelope,
            PasswordEnvelopePurpose purpose,
            PasswordEnvelopeChallengeContext context
    ) throws GeneralSecurityException {
        /** 与浏览器保持完全一致的附加认证数据。 */
        byte[] additionalData = additionalData(purpose, envelope.keyId(), envelope.nonce(), envelope.timestamp());
        /** PKCS#8 编码的临时 RSA 私钥。 */
        byte[] privateKeyBytes = Base64.getDecoder().decode(context.privateKey());
        /** RSA-OAEP 解包后的 AES-256 原始密钥。 */
        byte[] aesKeyBytes = null;
        /** AES-GCM 解密后的 UTF-8 密码字节。 */
        byte[] passwordBytes = null;
        try {
            /** 临时 RSA 私钥对象。 */
            java.security.PrivateKey privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            /** 使用 SHA-256 和 MGF1-SHA256 的 RSA-OAEP 解包器。 */
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(
                    Cipher.DECRYPT_MODE,
                    privateKey,
                    new OAEPParameterSpec(
                            "SHA-256",
                            "MGF1",
                            MGF1ParameterSpec.SHA256,
                            new PSource.PSpecified(additionalData)
                    )
            );
            aesKeyBytes = rsaCipher.doFinal(Base64.getDecoder().decode(envelope.encryptedKey()));
            if (aesKeyBytes.length != AES_KEY_BYTES) {
                throw new GeneralSecurityException("Invalid AES key length");
            }
            /** 客户端为当前密码随机生成的 GCM 初始向量。 */
            byte[] iv = Base64.getDecoder().decode(envelope.iv());
            if (iv.length != GCM_IV_BYTES) {
                throw new GeneralSecurityException("Invalid GCM IV length");
            }
            /** 带 128 位认证标签的 AES-GCM 解密器。 */
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
            aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKeyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            aesCipher.updateAAD(additionalData);
            passwordBytes = aesCipher.doFinal(Base64.getDecoder().decode(envelope.ciphertext()));
            /** 严格 UTF-8 解码后的密码。 */
            String password = decodeUtf8(passwordBytes);
            if (password.isBlank() || password.length() > MAX_PASSWORD_CHARACTERS) {
                throw new GeneralSecurityException("Invalid password length");
            }
            return password;
        } finally {
            Arrays.fill(privateKeyBytes, (byte) 0);
            if (aesKeyBytes != null) {
                Arrays.fill(aesKeyBytes, (byte) 0);
            }
            if (passwordBytes != null) {
                Arrays.fill(passwordBytes, (byte) 0);
            }
        }
    }

    /**
     * 严格解码密码 UTF-8 字节，拒绝替换非法字节序列。
     *
     * @param passwordBytes 密码 UTF-8 字节
     * @return 密码字符串
     * @throws GeneralSecurityException UTF-8 非法
     */
    private String decodeUtf8(byte[] passwordBytes) throws GeneralSecurityException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(passwordBytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new GeneralSecurityException("Invalid password encoding", exception);
        }
    }

    /**
     * 构建前后端共同认证的上下文，换行分隔避免字段拼接歧义。
     *
     * @param purpose 密码用途
     * @param keyId 临时密钥标识
     * @param nonce 一次性随机数
     * @param timestamp 服务端时间戳
     * @return UTF-8 附加认证数据
     */
    private byte[] additionalData(
            PasswordEnvelopePurpose purpose,
            String keyId,
            String nonce,
            long timestamp
    ) {
        return String.join("\n", purpose.name(), keyId, nonce, Long.toString(timestamp))
                .getBytes(StandardCharsets.UTF_8);
    }

    /** @param left 左侧文本 @param right 右侧文本 @return 是否恒定时间相等 */
    private boolean constantTimeEquals(String left, String right) {
        return left != null
                && right != null
                && MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param keyId RSA 密钥版本标识
     * @param nonce 一次性挑战随机数
     * @return 一次性挑战 Redis Key
     */
    private String challengeKey(String keyId, String nonce) {
        return redisKeyBuilder.build("auth", "password-envelope", "challenge", keyId, nonce);
    }

    /** @return 统一的无效密码信封业务异常 */
    private BusinessException invalidEnvelope() {
        return new BusinessException(INVALID_ENVELOPE_CODE, "密码加密挑战无效、已过期或已使用", 400);
    }

    /** @return 认证运行时依赖暂不可用业务异常 */
    private BusinessException runtimeUnavailable() {
        return new BusinessException(AUTH_RUNTIME_UNAVAILABLE_CODE, "认证运行状态暂时不可用", 503);
    }

    /**
     * Auth 进程内按周期复用的 RSA 密钥材料。
     *
     * @param keyId 密钥版本标识
     * @param keyPair RSA 公私钥对
     * @param expiresAtEpochMillis 密钥轮换毫秒时间戳
     */
    private record RotatingRsaKeyMaterial(
            String keyId,
            KeyPair keyPair,
            long expiresAtEpochMillis
    ) {
    }
}
