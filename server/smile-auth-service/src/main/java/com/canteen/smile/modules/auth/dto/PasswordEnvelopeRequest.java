package com.canteen.smile.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 浏览器生成的密码混合加密信封，所有二进制字段均使用标准 Base64。
 *
 * @param keyId 轮换周期内稳定的 RSA 公钥版本标识
 * @param nonce 服务端签发的一次性随机数
 * @param timestamp 服务端签发并由客户端原样绑定的毫秒时间戳
 * @param encryptedKey RSA-OAEP-SHA256 加密的 AES-256 原始密钥
 * @param iv AES-GCM 使用的 96 位随机初始向量
 * @param ciphertext AES-GCM 输出的密码密文和 128 位认证标签
 */
public record PasswordEnvelopeRequest(
        @NotBlank @Size(max = 64) String keyId,
        @NotBlank @Size(max = 128) String nonce,
        @Positive long timestamp,
        @NotBlank @Size(max = 1024) String encryptedKey,
        @NotBlank @Size(max = 64) String iv,
        @NotBlank @Size(max = 2048) String ciphertext
) {
}
