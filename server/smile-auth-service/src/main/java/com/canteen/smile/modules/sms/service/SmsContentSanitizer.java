package com.canteen.smile.modules.sms.service;

import org.springframework.stereotype.Component;

/** 在短信正文进入数据库或日志前替换验证码、Token 等一次性秘密。 */
@Component
public class SmsContentSanitizer {

    /** 脱敏后的统一占位文本。 */
    private static final String MASK = "******";

    /**
     * 替换正文中由业务调用方明确声明的全部敏感值。
     *
     * @param content 已渲染短信正文
     * @param sensitiveValues 验证码、Token 等一次性秘密
     * @param mobile 完整手机号
     * @param maskedMobile 脱敏手机号
     * @return 可写入投递记录和普通日志的正文
     */
    public String sanitize(
            String content,
            Iterable<String> sensitiveValues,
            String mobile,
            String maskedMobile
    ) {
        return sanitize(content, sensitiveValues, mobile, maskedMobile, false);
    }

    /**
     * 根据数据库安全策略生成正文快照；完整手机号始终脱敏。
     *
     * @param content 已渲染正文
     * @param sensitiveValues 验证码等一次性秘密
     * @param mobile 完整手机号
     * @param maskedMobile 脱敏手机号
     * @param retainSensitiveValues 是否允许数据库快照保留一次性秘密
     * @return 安全正文快照
     */
    public String sanitize(
            String content,
            Iterable<String> sensitiveValues,
            String mobile,
            String maskedMobile,
            boolean retainSensitiveValues
    ) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("SMS rendered content must not be blank");
        }
        if (sensitiveValues == null) {
            throw new IllegalArgumentException("SMS sensitive values must not be null");
        }
        String sanitized = content;
        boolean hasSensitiveValue = false;
        for (String sensitiveValue : sensitiveValues) {
            if (sensitiveValue == null || sensitiveValue.isBlank()) continue;
            hasSensitiveValue = true;
            if (!retainSensitiveValues) sanitized = sanitized.replace(sensitiveValue, MASK);
        }
        if (mobile != null && !mobile.isBlank() && maskedMobile != null && !maskedMobile.isBlank()) {
            sanitized = sanitized.replace(mobile.trim(), maskedMobile);
        }
        if (!hasSensitiveValue) {
            throw new IllegalArgumentException("SMS sensitive values must not be empty");
        }
        if (sanitized.length() > 1000) {
            throw new IllegalArgumentException("SMS sanitized content is too long");
        }
        return sanitized;
    }
}
