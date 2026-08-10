package com.canteen.smile.modules.sms.service;

import com.canteen.smile.config.SmsProperties;
import com.canteen.smile.modules.sms.model.SmsPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/** 生成六位短信验证码，并以带服务端 Pepper 的 HMAC 保存和校验摘要。 */
@Service
@RequiredArgsConstructor
public class SmsChallengeCodeService {

    /** HMAC-SHA256 算法名称。 */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** 验证码取值上界。 */
    private static final int CODE_BOUND = 1_000_000;

    /** 密码学安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /** 短信安全配置。 */
    private final SmsProperties smsProperties;

    /** @return 保留前导零的六位随机验证码 */
    public String generate() {
        /** 本次随机验证码数值。 */
        int value = secureRandom.nextInt(CODE_BOUND);
        return String.format(Locale.ROOT, "%06d", value);
    }

    /**
     * 计算绑定挑战、用途和手机号摘要的验证码 HMAC。
     *
     * @param challengeId 挑战标识
     * @param purpose 验证码用途
     * @param mobileHash 手机号摘要
     * @param code 六位验证码
     * @return HMAC-SHA256 十六进制摘要
     */
    public String hash(String challengeId, SmsPurpose purpose, String mobileHash, String code) {
        String payload = "sms-challenge:v1\n"
                + challengeId + "\n"
                + purpose.name() + "\n"
                + mobileHash + "\n"
                + code;
        try {
            /** 验证码摘要计算器。 */
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(resolvePepper().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SMS verification code hash could not be calculated", exception);
        }
    }

    /**
     * 使用常量时间比较校验验证码。
     *
     * @param expectedHash 数据库验证码摘要
     * @param challengeId 挑战标识
     * @param purpose 验证码用途
     * @param mobileHash 手机号摘要
     * @param code 用户提交验证码
     * @return 验证码是否匹配
     */
    public boolean matches(
            String expectedHash,
            String challengeId,
            SmsPurpose purpose,
            String mobileHash,
            String code
    ) {
        if (expectedHash == null || code == null || !code.matches("^[0-9]{6}$")) {
            return false;
        }
        /** 用户提交验证码的 HMAC 摘要。 */
        String actualHash = hash(challengeId, purpose, mobileHash, code);
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII),
                actualHash.getBytes(StandardCharsets.US_ASCII)
        );
    }

    /** @return 验证码 HMAC Pepper，未单独配置时显式回退到手机号 Pepper */
    private String resolvePepper() {
        String pepper = smsProperties.getCodeHashPepper();
        if (pepper == null || pepper.isBlank()) {
            pepper = smsProperties.getMobileHashPepper();
        }
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("SMS verification code hash pepper is not configured");
        }
        return pepper;
    }
}
