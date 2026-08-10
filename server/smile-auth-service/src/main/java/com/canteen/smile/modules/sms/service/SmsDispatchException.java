package com.canteen.smile.modules.sms.service;

/** 短信策略选择、重复领取或发送失败时使用的内部异常。 */
public class SmsDispatchException extends RuntimeException {

    /** 未找到已配置短信策略。 */
    public static final String PROVIDER_NOT_AVAILABLE = "SMS_PROVIDER_NOT_AVAILABLE";

    /** 请求 ID 已被领取，禁止重复发送。 */
    public static final String DUPLICATE_REQUEST = "SMS_DUPLICATE_REQUEST";

    /** 短信策略执行失败。 */
    public static final String DELIVERY_FAILED = "SMS_DELIVERY_FAILED";

    /** 稳定内部失败编码。 */
    private final String failureCode;

    /**
     * 创建不携带底层异常的短信分发异常。
     *
     * @param failureCode 稳定内部失败编码
     * @param message 脱敏错误说明
     */
    public SmsDispatchException(String failureCode, String message) {
        super(message);
        this.failureCode = failureCode;
    }

    /**
     * 创建保留底层异常链路的短信分发异常。
     *
     * @param failureCode 稳定内部失败编码
     * @param message 脱敏错误说明
     * @param cause 底层异常
     */
    public SmsDispatchException(String failureCode, String message, Throwable cause) {
        super(message, cause);
        this.failureCode = failureCode;
    }

    /**
     * 返回稳定内部失败编码。
     *
     * @return 失败编码
     */
    public String failureCode() {
        return failureCode;
    }
}
