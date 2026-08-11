package com.canteen.smile.modules.auth.service;

import com.canteen.smile.config.MobileEncryptionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** 使用可轮换 AES-256-GCM 密钥加密手机号，数据库不保存可读完整号码。 */
@Service
@RequiredArgsConstructor
public class MobileCipherService {

    /** 当前密文格式版本。 */
    private static final byte FORMAT_VERSION = 1;

    /** AES-GCM 随机 IV 字节数。 */
    private static final int IV_LENGTH = 12;

    /** GCM 认证标签位数。 */
    private static final int TAG_LENGTH_BITS = 128;

    /** AES-256 密钥字节数。 */
    private static final int KEY_LENGTH = 32;

    /** 认证加密算法。 */
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    /** 手机号加密密钥配置。 */
    private final MobileEncryptionProperties properties;

    /** 安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** 在发送绑定验证码前快速校验加密配置，避免验证码成功后才发现无法持久化。 */
    public void ensureConfigured() {
        requiredKeyId();
        decodedKey();
    }

    /**
     * 加密最小归一化后的完整手机号。
     *
     * @param normalizedMobile 仅存在于当前 Auth 调用栈的完整手机号
     * @return 密文载荷和密钥版本
     */
    public EncryptedMobile encrypt(String normalizedMobile) {
        /** 当前密钥版本标识。 */
        String keyId = requiredKeyId();
        /** Base64 解码后的 AES-256 密钥。 */
        byte[] key = decodedKey();
        /** 每条密文独立随机 IV。 */
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(keyId.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(normalizedMobile.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(1 + iv.length + ciphertext.length)
                    .put(FORMAT_VERSION)
                    .put(iv)
                    .put(ciphertext)
                    .array();
            return new EncryptedMobile(payload, keyId);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Mobile number could not be encrypted", exception);
        }
    }

    /** @return 非空且满足数据库长度约束的密钥版本 */
    private String requiredKeyId() {
        String keyId = properties.getKeyId();
        if (keyId == null || keyId.isBlank() || keyId.trim().length() > 128) {
            throw new IllegalStateException("Auth mobile encryption key ID is not configured");
        }
        return keyId.trim();
    }

    /** @return 严格为 32 字节的 AES-256 密钥 */
    private byte[] decodedKey() {
        String configuredKey = properties.getKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("Auth mobile encryption key is not configured");
        }
        try {
            byte[] key = Base64.getDecoder().decode(configuredKey.trim());
            if (key.length != KEY_LENGTH) {
                throw new IllegalStateException("Auth mobile encryption key must contain 32 bytes");
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Auth mobile encryption key must be valid Base64", exception);
        }
    }

    /**
     * 手机号认证密文。
     *
     * @param ciphertext 格式版本、IV、认证密文和标签的组合载荷
     * @param keyId 加密密钥版本
     */
    public record EncryptedMobile(byte[] ciphertext, String keyId) {
    }
}
