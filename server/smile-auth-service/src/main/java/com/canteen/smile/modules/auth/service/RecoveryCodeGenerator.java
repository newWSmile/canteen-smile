package com.canteen.smile.modules.auth.service;

import com.canteen.smile.infrastructure.security.HmacRequestSigner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/** 高熵平台恢复码生成和摘要组件。 */
@Component
public class RecoveryCodeGenerator {

    /** 密码学安全随机数生成器。 */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成 128 位随机恢复码并按四字符分组展示。
     *
     * @return 只允许向用户展示一次的恢复码
     */
    public String generate() {
        /** 128 位恢复码随机字节。 */
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        /** 未分组的小写十六进制恢复码。 */
        String compact = HexFormat.of().formatHex(bytes);
        return String.join("-", compact.split("(?<=\\G.{4})"));
    }

    /**
     * 计算去分隔符、转小写后的恢复码摘要。
     *
     * @param recoveryCode 用户提交的恢复码
     * @return SHA-256 小写十六进制摘要
     */
    public String hash(String recoveryCode) {
        /** 去除展示分隔符后的规范恢复码。 */
        String normalized = recoveryCode.replace("-", "").strip().toLowerCase(Locale.ROOT);
        return HmacRequestSigner.sha256Hex(normalized.getBytes(StandardCharsets.UTF_8));
    }
}
