package com.canteen.smile.infrastructure.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

/** 内部 HTTP 请求 HMAC-SHA256 规范串、摘要和签名工具。 */
public final class HmacRequestSigner {

    /** HMAC-SHA256 JCA 算法名称。 */
    private static final String HMAC_SHA_256 = "HmacSHA256";

    /** SHA-256 JCA 算法名称。 */
    private static final String SHA_256 = "SHA-256";

    /** 禁止实例化签名工具类。 */
    private HmacRequestSigner() {
    }

    /**
     * 计算请求体 SHA-256 小写十六进制摘要。
     *
     * @param content 请求体原始字节
     * @return 小写十六进制摘要
     */
    public static String sha256Hex(byte[] content) {
        try {
            /** SHA-256 消息摘要实例。 */
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK does not provide SHA-256", exception);
        }
    }

    /**
     * 生成内部请求签名。
     *
     * @param secret HMAC 密钥
     * @param canonicalRequest 已生成的规范请求串
     * @return 小写十六进制 HMAC 签名
     */
    public static String sign(String secret, String canonicalRequest) {
        try {
            /** HMAC-SHA256 计算实例。 */
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return HexFormat.of().formatHex(mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate internal request signature", exception);
        }
    }

    /**
     * 构建 HMAC v1 规范请求串。
     *
     * @param method HTTP 方法
     * @param path 原始请求路径
     * @param rawQuery 原始查询串，可以为空
     * @param contentSha256 请求体摘要
     * @param timestampEpochSeconds Unix 秒级时间戳
     * @param nonce 单次随机数
     * @param callerId 调用方服务标识
     * @param eventId 请求事件 ID
     * @return 按固定换行顺序拼接的规范请求串
     */
    public static String canonicalRequest(
            String method,
            String path,
            String rawQuery,
            String contentSha256,
            String timestampEpochSeconds,
            String nonce,
            String callerId,
            String eventId
    ) {
        return String.join(
                "\n",
                method.toUpperCase(Locale.ROOT),
                path,
                canonicalQuery(rawQuery),
                contentSha256,
                timestampEpochSeconds,
                nonce,
                callerId,
                eventId
        );
    }

    /**
     * 使用常量时间比较十六进制签名，降低时序侧信道风险。
     *
     * @param expected 服务端计算的期望签名
     * @param provided 请求携带的签名
     * @return 两个签名是否一致
     */
    public static boolean constantTimeEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                provided.getBytes(StandardCharsets.US_ASCII)
        );
    }

    /**
     * 对原始查询参数片段执行稳定排序。
     *
     * @param rawQuery 原始查询串
     * @return 排序后的规范查询串
     */
    private static String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        /** 按编码后 key=value 片段排序的查询参数。 */
        String[] parts = rawQuery.split("&", -1);
        Arrays.sort(parts);
        return String.join("&", parts);
    }
}
