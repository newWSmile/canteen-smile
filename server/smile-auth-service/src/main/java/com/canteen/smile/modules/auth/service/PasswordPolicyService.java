package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** 默认企业密码策略校验服务。 */
@Service
public class PasswordPolicyService {

    /** 密码策略校验失败错误码。 */
    private static final String PASSWORD_POLICY_CODE = "AUTH_1009";

    /** 禁止使用的常见弱密码归一化集合。 */
    private static final Set<String> COMMON_WEAK_PASSWORDS = Set.of(
            "password",
            "password123",
            "admin123",
            "12345678",
            "qwerty123",
            "abc123456"
    );

    /**
     * 校验默认密码策略和用户名包含关系。
     *
     * @param rawPassword 原始密码
     * @param username 当前用户名
     */
    public void validate(String rawPassword, String username) {
        /** 当前密码满足的字符类别数量。 */
        int categoryCount = 0;
        categoryCount += rawPassword.chars().anyMatch(Character::isDigit) ? 1 : 0;
        categoryCount += rawPassword.chars().anyMatch(Character::isUpperCase) ? 1 : 0;
        categoryCount += rawPassword.chars().anyMatch(Character::isLowerCase) ? 1 : 0;
        categoryCount += rawPassword.chars().anyMatch(character -> !Character.isLetterOrDigit(character)) ? 1 : 0;
        /** 用于弱密码和用户名比较的归一化密码。 */
        String normalizedPassword = normalize(rawPassword);
        /** 用于包含关系比较的归一化用户名。 */
        String normalizedUsername = normalize(username);
        if (rawPassword.length() < 8
                || rawPassword.length() > 128
                || categoryCount < 3
                || COMMON_WEAK_PASSWORDS.contains(normalizedPassword)
                || normalizedPassword.contains(normalizedUsername)) {
            throw new BusinessException(
                    PASSWORD_POLICY_CODE,
                    "密码至少 8 位并满足数字、大小写字母、特殊字符中的三类，且不能包含用户名或常见弱密码"
            );
        }
    }

    /**
     * 归一化密码策略比较文本。
     *
     * @param value 原始文本
     * @return NFKC 小写文本
     */
    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
}
