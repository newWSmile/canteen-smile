package com.canteen.smile.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Auth 各环境 YAML 配置语法测试。 */
class AuthEnvironmentYamlSyntaxTest {

    /** 验证 dev、test、prod、local 配置都可以被 SnakeYAML 解析。 */
    @Test
    void shouldParseAllEnvironmentYamlFiles() throws IOException {
        /** 需要持续校验的 Auth 环境配置资源。 */
        List<String> resources = List.of(
                "application-dev.yml",
                "application-test.yml",
                "application-prod.yml",
                "application-local.yml"
        );
        /** 与 Spring Boot 使用相同语法规则的 YAML 解析器。 */
        Yaml yaml = new Yaml();
        for (String resource : resources) {
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertThat(inputStream).as(resource).isNotNull();
            // 当前环境 YAML 的解析结果。
            Object parsedYaml = yaml.load(inputStream);
            assertThat(parsedYaml).as(resource).isNotNull();
            }
        }
    }
}
