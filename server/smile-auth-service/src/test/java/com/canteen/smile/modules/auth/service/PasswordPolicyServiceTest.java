package com.canteen.smile.modules.auth.service;

import com.canteen.smile.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 默认企业密码策略测试。 */
class PasswordPolicyServiceTest {

    /** 被测试密码策略。 */
    private final PasswordPolicyService service = new PasswordPolicyService();

    /** 验证满足四类字符且不包含用户名的密码通过。 */
    @Test
    void shouldAcceptStrongPassword() {
        assertThatCode(() -> service.validate("River!Stone2026", "platform-root"))
                .doesNotThrowAnyException();
    }

    /** 验证包含用户名的密码被稳定错误码拒绝。 */
    @Test
    void shouldRejectPasswordContainingUsername() {
        assertThatThrownBy(() -> service.validate("AdminRoot!2026", "adminroot"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("AUTH_1009");
    }
}
