package com.canteen.smile.api;

import com.canteen.smile.common.api.AuthPublicApiPaths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Auth 外部路径和公共白名单一致性测试。 */
class AuthApiPathsTest {

    /** 验证 Gateway、WebMVC 与 Auth Controller 复用同一公开路径契约。 */
    @Test
    void shouldSharePublicPathContract() {
        assertThat(AuthApiPaths.PLATFORM_BOOTSTRAP).isEqualTo(AuthPublicApiPaths.PLATFORM_BOOTSTRAP);
        assertThat(AuthApiPaths.PASSWORD_ENCRYPTION_CHALLENGES)
                .isEqualTo(AuthPublicApiPaths.PASSWORD_ENCRYPTION_CHALLENGES);
        assertThat(AuthPublicApiPaths.ANONYMOUS_PATHS)
                .contains(AuthApiPaths.PASSWORD_ENCRYPTION_CHALLENGES);
        assertThat(AuthApiPaths.SMS_CHALLENGES).isEqualTo(AuthPublicApiPaths.SMS_CHALLENGES);
        assertThat(AuthPublicApiPaths.ANONYMOUS_PATHS).contains(AuthApiPaths.SMS_CHALLENGES);
        assertThat(AuthApiPaths.PASSWORD_LOGIN).isEqualTo(AuthPublicApiPaths.PASSWORD_LOGIN);
        assertThat(AuthPublicApiPaths.ANONYMOUS_PATHS)
                .contains(AuthApiPaths.SMS_LOGIN, AuthApiPaths.ACCOUNT_SELECTION_LOGIN);
        assertThat(AuthApiPaths.PLATFORM_RECOVERY_LOGIN)
                .isEqualTo(AuthPublicApiPaths.PLATFORM_RECOVERY_LOGIN);
        assertThat(AuthPublicApiPaths.isAnonymous(AuthApiPaths.PASSWORD_RESET_SMS_VERIFICATION))
                .isTrue();
        assertThat(AuthPublicApiPaths.isAnonymous(AuthApiPaths.PASSWORD_RESET_SMS_ACCOUNT_SELECTION))
                .isTrue();
        assertThat(AuthApiPaths.INTERNAL_SECURITY_EVENTS)
                .isEqualTo("/internal/auth/v1/security-events");
        assertThat(AuthApiPaths.INTERNAL_AUDIT_LOG_SEARCH)
                .isEqualTo("/internal/auth/v1/audit-logs/search");
        assertThat(AuthPublicApiPaths.ANONYMOUS_PATHS)
                .doesNotContain(
                        AuthApiPaths.MOBILE_BINDING,
                        AuthApiPaths.MOBILE_BINDING_CHALLENGES,
                        AuthApiPaths.MOBILE_BINDING_CONFIRM,
                        AuthApiPaths.MOBILE_BINDING_CURRENT_CHALLENGES,
                        AuthApiPaths.MOBILE_BINDING_CURRENT_VERIFICATION,
                        AuthApiPaths.MOBILE_BINDING_CHANGE_CHALLENGES,
                        AuthApiPaths.MOBILE_BINDING_CHANGE_CONFIRM,
                        AuthApiPaths.MOBILE_BINDING_UNBIND_CONFIRM
                );
    }
}
