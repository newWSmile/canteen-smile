package com.canteen.smile.modules.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 平台恢复码生成与摘要测试。 */
class RecoveryCodeGeneratorTest {

    /** 验证恢复码具备 128 位随机载荷且摘要忽略展示分隔符和大小写。 */
    @Test
    void shouldGenerateAndNormalizeRecoveryCode() {
        /** 被测试的恢复码组件。 */
        RecoveryCodeGenerator generator = new RecoveryCodeGenerator();
        /** 新生成恢复码。 */
        String recoveryCode = generator.generate();

        assertThat(recoveryCode).matches("^[0-9a-f]{4}(?:-[0-9a-f]{4}){7}$");
        assertThat(generator.hash(recoveryCode))
                .isEqualTo(generator.hash(recoveryCode.replace("-", "").toUpperCase()));
        assertThat(generator.generate()).isNotEqualTo(recoveryCode);
    }
}
