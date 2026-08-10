package com.canteen.smile.modules.sms.vo;

import java.time.OffsetDateTime;

/**
 * 短信验证码挑战创建结果。
 *
 * @param challengeId 外部随机挑战标识
 * @param maskedMobile 脱敏手机号
 * @param expiresAt 验证码失效时间
 * @param resendAt 允许再次发送的时间
 */
public record SmsChallengeVO(
        String challengeId,
        String maskedMobile,
        OffsetDateTime expiresAt,
        OffsetDateTime resendAt
) {
}
