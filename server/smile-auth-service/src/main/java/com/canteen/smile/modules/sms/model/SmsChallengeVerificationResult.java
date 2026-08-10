package com.canteen.smile.modules.sms.model;

/**
 * 已成功一次性消费的短信验证码安全上下文。
 *
 * @param challengeId 已消费挑战标识
 * @param purpose 已校验用途
 * @param mobileHash 已验证手机号摘要
 */
public record SmsChallengeVerificationResult(
        String challengeId,
        SmsPurpose purpose,
        String mobileHash
) {
}
