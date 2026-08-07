package com.canteen.smile.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 业务异常稳定契约单元测试。 */
class BusinessExceptionTest {

    /** 验证旧构造方式继续使用 400 状态。 */
    @Test
    void shouldUseBadRequestAsDefaultHttpStatus() {
        /** 使用默认状态创建的业务异常。 */
        BusinessException exception = new BusinessException("IAM_2004", "机构关系不合法");

        assertThat(exception.getCode()).isEqualTo("IAM_2004");
        assertThat(exception.getHttpStatus()).isEqualTo(400);
    }

    /** 验证冲突类业务错误可以保留明确 HTTP 状态。 */
    @Test
    void shouldKeepExplicitHttpStatus() {
        /** 使用冲突状态创建的业务异常。 */
        BusinessException exception = new BusinessException("IAM_2006", "资源版本冲突", 409);

        assertThat(exception.getHttpStatus()).isEqualTo(409);
    }

    /** 验证成功状态不能被错误地用于业务异常。 */
    @Test
    void shouldRejectSuccessHttpStatus() {
        assertThatThrownBy(() -> new BusinessException("INVALID", "无效状态", 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTP status");
    }
}
