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
        assertThat(AuthApiPaths.PLATFORM_RECOVERY_LOGIN)
                .isEqualTo(AuthPublicApiPaths.PLATFORM_RECOVERY_LOGIN);
        assertThat(AuthApiPaths.INTERNAL_SECURITY_EVENTS)
                .isEqualTo("/internal/auth/v1/security-events");
        assertThat(AuthApiPaths.INTERNAL_AUDIT_LOG_SEARCH)
                .isEqualTo("/internal/auth/v1/audit-logs/search");
    }
}
