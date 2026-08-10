package com.canteen.smile.modules.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.modules.audit.dto.AuditLogPageQuery;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** 平台与租户审计接口权限注解契约测试。 */
class AuditLogControllerPermissionTest {

    /** 验证平台审计使用集中平台权限码。 */
    @Test
    void shouldDeclarePlatformAuditPermission() throws NoSuchMethodException {
        Method method = PlatformAuditLogController.class.getMethod("page", AuditLogPageQuery.class);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.PLATFORM_AUDIT_VIEW);
    }

    /** 验证租户审计使用集中租户权限码。 */
    @Test
    void shouldDeclareTenantAuditPermission() throws NoSuchMethodException {
        Method method = TenantAuditLogController.class.getMethod("page", AuditLogPageQuery.class);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.IAM_AUDIT_VIEW);
    }
}
