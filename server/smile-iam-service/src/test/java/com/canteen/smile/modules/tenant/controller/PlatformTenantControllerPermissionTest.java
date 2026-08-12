package com.canteen.smile.modules.tenant.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.tenant.dto.TenantPageQuery;
import com.canteen.smile.modules.tenant.dto.CreateTenantRequest;
import com.canteen.smile.modules.tenant.dto.TenantOwnerPasswordResetRequest;
import com.canteen.smile.modules.tenant.dto.PlatformTenantStatusRequest;
import com.canteen.smile.modules.tenant.dto.UpdatePlatformTenantRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** 平台租户接口权限注解契约测试。 */
class PlatformTenantControllerPermissionTest {

    /** 验证 Controller 使用集中权限常量声明最终权限校验。 */
    @Test
    void shouldDeclarePlatformTenantViewPermission() throws NoSuchMethodException {
        /** 租户分页接口方法。 */
        Method method = PlatformTenantController.class.getMethod("pageTenants", TenantPageQuery.class);
        /** Sa-Token 接口权限注解。 */
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.PLATFORM_TENANT_VIEW);
    }

    /** 验证租户创建接口声明集中维护的创建权限。 */
    @Test
    void shouldDeclarePlatformTenantCreatePermission() throws NoSuchMethodException {
        /** 租户创建接口方法。 */
        Method method = PlatformTenantController.class.getMethod(
                "createTenant", String.class, CreateTenantRequest.class);
        /** Sa-Token 接口权限注解。 */
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.PLATFORM_TENANT_CREATE);
    }

    /** 验证租户所有者密码恢复必须声明集中维护的密码重置权限。 */
    @Test
    void shouldDeclareTenantOwnerPasswordResetPermission() throws NoSuchMethodException {
        /** 所有者密码恢复链接签发方法。 */
        Method method = PlatformTenantController.class.getMethod(
                "issueOwnerPasswordResetLink",
                long.class,
                TenantOwnerPasswordResetRequest.class
        );
        /** Sa-Token 接口权限注解。 */
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.IAM_USER_PASSWORD_RESET);
    }

    /** 验证租户资料修改接口声明独立修改权限。 */
    @Test
    void shouldDeclarePlatformTenantUpdatePermission() throws NoSuchMethodException {
        Method method = PlatformTenantController.class.getMethod(
                "updateTenant", long.class, UpdatePlatformTenantRequest.class);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.PLATFORM_TENANT_UPDATE);
    }

    /** 验证租户暂停与恢复接口复用集中维护的状态治理权限。 */
    @Test
    void shouldDeclarePlatformTenantStatusPermission() throws NoSuchMethodException {
        for (String methodName : java.util.List.of("suspendTenant", "resumeTenant")) {
            Method method = PlatformTenantController.class.getMethod(
                    methodName, long.class, PlatformTenantStatusRequest.class);
            SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

            assertThat(permission).isNotNull();
            assertThat(permission.value()).containsExactly(IamPermissionCodes.PLATFORM_TENANT_STATUS);
        }
    }

    /** 验证不可恢复注销接口使用独立高风险权限。 */
    @Test
    void shouldDeclarePlatformTenantCancelPermission() throws NoSuchMethodException {
        Method method = PlatformTenantController.class.getMethod(
                "cancelTenant", long.class, PlatformTenantStatusRequest.class);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.PLATFORM_TENANT_CANCEL);
    }
}
