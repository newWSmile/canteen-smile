package com.canteen.smile.modules.sms.service;

import com.canteen.smile.config.SmsProperties;
import com.canteen.smile.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** 统一完成手机号最小归一化、HMAC 查询摘要和脱敏展示。 */
@Service
@RequiredArgsConstructor
public class MobileProtectionService {

    /** HMAC-SHA256 算法名称。 */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** 中国大陆手机号：十一位数字，第一位为 1，第二位为 3 至 9。 */
    private static final Pattern MAINLAND_MOBILE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /** 手机号格式错误码。 */
    private static final String INVALID_MOBILE_CODE = "AUTH_1016";

    /** 短信安全配置。 */
    private final SmsProperties smsProperties;

    /**
     * 生成手机号的安全查询和展示形式。
     *
     * @param mobile 用户输入的完整手机号
     * @return 手机号安全投影
     */
    public ProtectedMobile protect(String mobile) {
        String normalizedMobile = normalize(mobile);
        return new ProtectedMobile(normalizedMobile, hash(normalizedMobile), mask(normalizedMobile));
    }

    /**
     * 只生成手机号查询摘要，供平台精确筛选使用。
     *
     * @param mobile 平台输入的完整手机号
     * @return HMAC-SHA256 十六进制摘要
     */
    public String hashForSearch(String mobile) {
        return hash(normalize(mobile));
    }

    /** 去除首尾空白并校验中国大陆十一位手机号格式。 */
    public String normalize(String mobile) {
        if (mobile == null) {
            throw invalidMobile();
        }
        /** 进入摘要计算和加密前的唯一规范手机号。 */
        String normalizedMobile = mobile.trim();
        if (!MAINLAND_MOBILE_PATTERN.matcher(normalizedMobile).matches()) {
            throw invalidMobile();
        }
        return normalizedMobile;
    }

    /** @return 不泄露校验内部细节的手机号格式异常 */
    private BusinessException invalidMobile() {
        return new BusinessException(INVALID_MOBILE_CODE, "请输入正确的11位手机号", 400);
    }

    /** 使用仅服务端持有的 Pepper 计算手机号查询摘要。 */
    private String hash(String normalizedMobile) {
        String pepper = smsProperties.getMobileHashPepper();
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("SMS mobile hash pepper is not configured");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(normalizedMobile.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SMS mobile hash could not be calculated", exception);
        }
    }

    /** 生成只保留前三位和后四位的展示值，短号码使用更严格的通用掩码。 */
    private String mask(String normalizedMobile) {
        if (normalizedMobile.length() >= 8) {
            return normalizedMobile.substring(0, 3)
                    + "****"
                    + normalizedMobile.substring(normalizedMobile.length() - 4);
        }
        return normalizedMobile.charAt(0) + "***" + normalizedMobile.charAt(normalizedMobile.length() - 1);
    }

    /**
     * 手机号安全投影。
     *
     * @param normalized 经过最小归一化的完整手机号，只能在 Auth 当前调用栈内使用
     * @param hash 查询摘要
     * @param masked 展示脱敏值
     */
    public record ProtectedMobile(String normalized, String hash, String masked) {
    }
}
