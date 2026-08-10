package com.canteen.smile.modules.outbox.client;

import lombok.Getter;

/** Outbox 单事件投递失败，携带可审计错误码与是否永久失败。 */
@Getter
public class OutboxDeliveryException extends RuntimeException {

    /** 可写入 Outbox 的非敏感错误码。 */
    private final String errorCode;

    /** 是否无需继续重试。 */
    private final boolean permanent;

    /** 创建投递异常。 */
    public OutboxDeliveryException(String errorCode, boolean permanent, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
        this.permanent = permanent;
    }

    /** 创建不包含底层异常的投递异常。 */
    public OutboxDeliveryException(String errorCode, boolean permanent) {
        this(errorCode, permanent, null);
    }
}
