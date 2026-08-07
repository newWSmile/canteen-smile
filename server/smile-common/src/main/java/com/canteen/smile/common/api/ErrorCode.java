package com.canteen.smile.common.api;

/** 系统公共错误码；业务模块应在明确需求后定义各自稳定的错误码。 */
public enum ErrorCode {

    /** 请求成功。 */
    SUCCESS("0", "操作成功"),

    /** 请求参数不符合约束。 */
    VALIDATION_FAILED("COMMON_400", "请求参数校验失败"),

    /** 当前会话尚未登录。 */
    UNAUTHORIZED("AUTH_401", "请先登录"),

    /** 当前会话缺少所需权限。 */
    FORBIDDEN("AUTH_403", "无权执行此操作"),

    /** 服务端未预期异常。 */
    INTERNAL_ERROR("COMMON_500", "系统繁忙，请稍后重试");

    /** 对外稳定错误码。 */
    private final String code;

    /** 对外默认提示。 */
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /** @return 对外稳定错误码 */
    public String getCode() {
        return code;
    }

    /** @return 对外默认提示 */
    public String getMessage() {
        return message;
    }
}
