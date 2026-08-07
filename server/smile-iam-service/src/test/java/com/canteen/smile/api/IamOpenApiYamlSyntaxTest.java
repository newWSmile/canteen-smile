package com.canteen.smile.api;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** IAM OpenAPI 文档 YAML 语法测试。 */
class IamOpenApiYamlSyntaxTest {

    /** 验证已经冻结的 IAM 外部契约可以被 YAML 解析器完整读取。 */
    @Test
    void shouldParseIamOpenApiYaml() throws IOException {
        /** Maven 从 server 根目录或 IAM 模块目录运行时的候选文档路径。 */
        Path serverRelative = Path.of("docs", "openapi", "iam-v1.yaml");
        Path moduleRelative = Path.of("..", "docs", "openapi", "iam-v1.yaml");
        Path openApi = Files.exists(serverRelative) ? serverRelative : moduleRelative;

        assertThat(openApi).exists();
        try (InputStream inputStream = Files.newInputStream(openApi)) {
            Object document = new Yaml().load(inputStream);
            assertThat(document).isNotNull();
        }
    }
}
