package com.canteen.smile.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 统一接口响应单元测试。 */
class ApiResponseTest {

    /** 验证成功响应的稳定协议字段。 */
    @Test
    void shouldCreateSuccessResponse() {
        String value = "verified";

        ApiResponse<String> response = ApiResponse.success(value);

        assertThat(response.code()).isEqualTo("0");
        assertThat(response.message()).isEqualTo("操作成功");
        assertThat(response.data()).isEqualTo(value);
        assertThat(response.timestamp()).isNotNull();
    }
}
