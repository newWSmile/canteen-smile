package com.canteen.smile.infrastructure.security;

/** 内部 HMAC v1 请求头名称。 */
public final class InternalHmacHeaders {

    /** 调用方服务标识请求头。 */
    public static final String CALLER_ID = "X-Caller-Id";

    /** HMAC 密钥版本请求头。 */
    public static final String KEY_ID = "X-Key-Id";

    /** Unix 秒级时间戳请求头。 */
    public static final String TIMESTAMP = "X-Timestamp";

    /** 防重放随机数请求头。 */
    public static final String NONCE = "X-Nonce";

    /** 请求事件 ID 请求头。 */
    public static final String EVENT_ID = "X-Event-Id";

    /** 请求体 SHA-256 摘要请求头。 */
    public static final String CONTENT_SHA_256 = "X-Content-SHA256";

    /** HMAC-SHA256 签名请求头。 */
    public static final String SIGNATURE = "X-Signature";

    /** 禁止实例化请求头常量类。 */
    private InternalHmacHeaders() {
    }
}
