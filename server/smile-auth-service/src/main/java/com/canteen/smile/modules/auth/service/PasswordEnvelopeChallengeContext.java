package com.canteen.smile.modules.auth.service;

/**
 * 仅在 Redis 短期保存的密码加密挑战服务端上下文。
 *
 * @param keyId 轮换周期内稳定的 RSA 密钥标识
 * @param purpose 挑战绑定的业务用途
 * @param nonce 一次性随机数
 * @param timestamp 服务端签发的毫秒时间戳
 * @param expiresAtEpochMillis 挑战失效毫秒时间戳
 * @param privateKey Base64 编码的 PKCS#8 RSA 私钥，仅随挑战短期保存
 */
public record PasswordEnvelopeChallengeContext(
        String keyId,
        String purpose,
        String nonce,
        long timestamp,
        long expiresAtEpochMillis,
        String privateKey
) {
}
