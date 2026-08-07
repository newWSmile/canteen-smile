package com.canteen.smile.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.canteen.smile.common.api.ApiResponse;
import com.canteen.smile.common.api.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 将框架及业务异常转换为统一响应。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 当前类日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理请求体字段校验异常。
     *
     * @param exception 参数校验异常
     * @return 400 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? ErrorCode.VALIDATION_FAILED.getMessage() : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResponse.failure(ErrorCode.VALIDATION_FAILED, message));
    }

    /**
     * 处理路径或查询参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 400 统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(
                ApiResponse.failure(ErrorCode.VALIDATION_FAILED, exception.getMessage())
        );
    }

    /**
     * 处理未登录异常。
     *
     * @param exception Sa-Token 未登录异常
     * @return 401 统一响应
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException exception) {
        log.debug("Unauthenticated request: {}", exception.getType());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage()));
    }

    /**
     * 处理接口或角色权限异常。
     *
     * @param exception Sa-Token 权限异常
     * @return 403 统一响应
     */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(RuntimeException exception) {
        log.debug("Forbidden request: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage()));
    }

    /**
     * 处理明确业务异常。
     *
     * @param exception 业务异常
     * @return 业务失败统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.getHttpStatus())
                .body(ApiResponse.failure(exception.getCode(), exception.getMessage()));
    }

    /**
     * 兜底处理未预期异常，避免向客户端泄漏内部细节。
     *
     * @param exception 未预期异常
     * @return 500 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unexpected server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
