package com.canteen.smile.common.exception;

/** 携带稳定错误码的业务异常。 */
public class BusinessException extends RuntimeException {

    /** 业务异常默认使用的 HTTP 状态码。 */
    private static final int DEFAULT_HTTP_STATUS = 400;

    /** 对外稳定业务错误码。 */
    private final String code;

    /** 应向调用方返回的 HTTP 状态码。 */
    private final int httpStatus;

    /**
     * 创建业务异常。
     *
     * @param code 业务错误码
     * @param message 对外错误信息
     */
    public BusinessException(String code, String message) {
        this(code, message, DEFAULT_HTTP_STATUS);
    }

    /**
     * 创建带明确 HTTP 状态的业务异常。
     *
     * @param code 业务错误码
     * @param message 对外错误信息
     * @param httpStatus HTTP 状态码，必须是 4xx 或 5xx
     */
    public BusinessException(String code, String message, int httpStatus) {
        super(message);
        if (httpStatus < 400 || httpStatus > 599) {
            throw new IllegalArgumentException("Business exception HTTP status must be between 400 and 599");
        }
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /** @return 业务错误码 */
    public String getCode() {
        return code;
    }

    /** @return 应向调用方返回的 HTTP 状态码 */
    public int getHttpStatus() {
        return httpStatus;
    }
}
