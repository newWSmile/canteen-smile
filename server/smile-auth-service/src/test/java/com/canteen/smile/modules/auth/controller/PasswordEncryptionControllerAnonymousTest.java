package com.canteen.smile.modules.auth.controller;

import com.canteen.smile.api.AuthApiPaths;
import com.canteen.smile.config.SaTokenConfiguration;
import com.canteen.smile.modules.auth.model.PasswordEnvelopePurpose;
import com.canteen.smile.modules.auth.service.PasswordEnvelopeService;
import com.canteen.smile.modules.auth.vo.PasswordEncryptionChallengeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Auth MVC 最终鉴权层的密码挑战匿名访问测试。 */
@WebMvcTest(PasswordEncryptionController.class)
@ContextConfiguration(classes = {
        PasswordEncryptionController.class,
        SaTokenConfiguration.class
})
class PasswordEncryptionControllerAnonymousTest {

    /** MVC 测试客户端。 */
    @Autowired
    private MockMvc mockMvc;

    /** 模拟密码信封服务，测试只关注 MVC 鉴权边界。 */
    @MockitoBean
    private PasswordEnvelopeService passwordEnvelopeService;

    /** 验证挑战请求通过 Auth 自身 Sa-Token 拦截器，无需预先登录。 */
    @Test
    void shouldAllowAnonymousChallengeRequest() throws Exception {
        /** 模拟服务层返回的短期公钥挑战。 */
        PasswordEncryptionChallengeVO challenge = new PasswordEncryptionChallengeVO(
                PasswordEnvelopePurpose.PLATFORM_PASSWORD_LOGIN,
                "00000000-0000-0000-0000-000000000001",
                "public-key",
                "nonce",
                1L,
                OffsetDateTime.parse("2026-08-07T10:00:00+08:00"),
                "RSA-OAEP-256",
                "A256GCM"
        );
        when(passwordEnvelopeService.issue(PasswordEnvelopePurpose.PLATFORM_PASSWORD_LOGIN))
                .thenReturn(challenge);

        mockMvc.perform(post(AuthApiPaths.PASSWORD_ENCRYPTION_CHALLENGES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purpose\":\"PLATFORM_PASSWORD_LOGIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.keyId").value(challenge.keyId()));
    }
}
