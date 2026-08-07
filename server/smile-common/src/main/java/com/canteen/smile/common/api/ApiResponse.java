package com.canteen.smile.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.OffsetDateTime;

/**
 * REST 接口统一响应结构。
 *
 * @param code 错误码，0 表示成功
 * @param message 人类可读提示
 * @param data 响应数据
 * @param timestamp 响应时间
 * @param traceId 链路追踪标识
 * @param <T> 响应数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        OffsetDateTime timestamp,
        String traceId
) {

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data,
                OffsetDateTime.now(),
                MDC.get(TraceConstants.TRACE_ID)
        );
    }

    /**
     * 创建失败响应。
     *
     * @param errorCode 公共错误码
     * @param message 对外提示
     * @return 无数据失败响应
     */
    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return failure(errorCode.getCode(), message);
    }

    /**
     * 创建业务失败响应。
     *
     * @param code 业务错误码
     * @param message 对外提示
     * @return 无数据失败响应
     */
    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, OffsetDateTime.now(), MDC.get(TraceConstants.TRACE_ID));
    }
}
