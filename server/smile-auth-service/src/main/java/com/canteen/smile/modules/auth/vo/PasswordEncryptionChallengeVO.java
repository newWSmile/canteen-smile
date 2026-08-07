package com.canteen.smile.modules.auth.vo;

import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;

import java.time.OffsetDateTime;

/**
 * 浏览器创建一次性密码信封所需的轮换公钥和短期挑战。
 *
 * @param purpose 挑战绑定的业务用途
 * @param keyId 轮换周期内稳定的 RSA 公钥版本标识
 * @param publicKey Base64 编码的 X.509 SubjectPublicKeyInfo 公钥
 * @param nonce 一次性随机数
 * @param timestamp 服务端签发的毫秒时间戳
 * @param expiresAt 挑战失效时间
 * @param keyAlgorithm 密钥包装算法
 * @param contentAlgorithm 密码正文加密算法
 */
public record PasswordEncryptionChallengeVO(
        PasswordEnvelopePurpose purpose,
        String keyId,
        String publicKey,
        String nonce,
        long timestamp,
        OffsetDateTime expiresAt,
        String keyAlgorithm,
        String contentAlgorithm
) {
}
