package com.canteen.smile.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** IAM 服务路径分区契约测试。 */
class IamApiPathsTest {

    /** 验证外部和内部路径具有不同入口且均包含 IAM 服务标识。 */
    @Test
    void shouldKeepExternalAndInternalPathsSeparated() {
        assertThat(IamApiPaths.EXTERNAL_V1).isEqualTo("/api/iam/v1");
        assertThat(IamApiPaths.INTERNAL_V1).isEqualTo("/internal/iam/v1");
        assertThat(IamApiPaths.PLATFORM_TENANTS).startsWith(IamApiPaths.EXTERNAL_V1 + "/");
        assertThat(IamApiPaths.PLATFORM_TENANTS).doesNotStartWith(IamApiPaths.INTERNAL_V1);
        assertThat(IamApiPaths.TENANT_CONTEXT).startsWith(IamApiPaths.EXTERNAL_V1 + "/tenant/");
        assertThat(IamApiPaths.TENANT_ORGANIZATION_TYPES).startsWith(IamApiPaths.EXTERNAL_V1 + "/tenant/");
        assertThat(IamApiPaths.TENANT_ORGANIZATIONS).startsWith(IamApiPaths.EXTERNAL_V1 + "/tenant/");
        assertThat(IamApiPaths.PLATFORM_AUDIT_LOGS).isEqualTo("/api/iam/v1/platform/audit-logs");
        assertThat(IamApiPaths.TENANT_AUDIT_LOGS).isEqualTo("/api/iam/v1/tenant/audit-logs");
        assertThat(IamApiPaths.MOBILE_ACCOUNT_LOGIN_RESOLUTION)
                .isEqualTo("/internal/iam/v1/login-resolutions/mobile-accounts");
    }
}
