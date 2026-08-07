package com.canteen.smile.internal.client;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.dto.BootstrapPlatformIdentityInternalRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/** Auth 调用 IAM 平台身份内部接口的错误契约测试。 */
class IamPlatformIdentityClientTest {

    /** IAM 明确返回引导已关闭时，应转换为 Auth 的已初始化业务提示。 */
    @Test
    void shouldTranslateIamBootstrapClosedResponse() {
        /** 用于绑定模拟服务端的 RestClient 构建器。 */
        RestClient.Builder builder = RestClient.builder().baseUrl("http://iam-service");
        /** 模拟 IAM HTTP 服务端。 */
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        /** 待验证的 IAM 平台身份 Client。 */
        IamPlatformIdentityClient client = new IamPlatformIdentityClient(builder.build(), new ObjectMapper());
        server.expect(once(), requestTo("http://iam-service/internal/iam/v1/platform-identities/bootstrap"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"IAM_2013\",\"message\":\"首次引导已经关闭\"}"));

        assertThatThrownBy(() -> client.bootstrap(
                new BootstrapPlatformIdentityInternalRequest("second-admin", "第二位管理员")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("AUTH_1010");
            assertThat(exception.getHttpStatus()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("平台已经完成初始化，不能重复创建平台超级管理员");
        });
        server.verify();
    }
}
