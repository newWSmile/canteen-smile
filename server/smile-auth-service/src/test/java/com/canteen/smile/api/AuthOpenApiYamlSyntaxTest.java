package com.canteen.smile.api;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Auth OpenAPI 文档 YAML 语法与账号安全契约测试。 */
class AuthOpenApiYamlSyntaxTest {

    /** 验证 Auth 外部契约可解析，且包含密码找回和手机号安全流程。 */
    @Test
    void shouldParseAuthOpenApiYamlAndContainSmsPasswordRecoveryPaths() throws IOException {
        /** Maven 从 server 根目录或 Auth 模块目录运行时的候选文档路径。 */
        Path serverRelative = Path.of("docs", "openapi", "auth-v1.yaml");
        Path moduleRelative = Path.of("..", "docs", "openapi", "auth-v1.yaml");
        Path openApi = Files.exists(serverRelative) ? serverRelative : moduleRelative;

        assertThat(openApi).exists();
        try (InputStream inputStream = Files.newInputStream(openApi)) {
            Map<String, Object> document = new Yaml().load(inputStream);
            assertThat(document).isNotNull();
            assertThat(document.get("paths")).isInstanceOf(Map.class);
            Map<?, ?> paths = (Map<?, ?>) document.get("paths");
            assertThat(paths.containsKey("/password-resets/sms/verification")).isTrue();
            assertThat(paths.containsKey("/password-resets/sms/account-selection")).isTrue();
            assertThat(paths.containsKey("/mobile/binding/current-mobile/challenges")).isTrue();
            assertThat(paths.containsKey("/mobile/binding/current-mobile/verification")).isTrue();
            assertThat(paths.containsKey("/mobile/binding/change/challenges")).isTrue();
            assertThat(paths.containsKey("/mobile/binding/change/confirm")).isTrue();
            assertThat(paths.containsKey("/mobile/binding/unbind/confirm")).isTrue();
        }
    }
}
