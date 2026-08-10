package com.canteen.smile.modules.account.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.modules.account.dto.ReplaceTenantUserRolesRequest;
import com.canteen.smile.modules.account.dto.TenantUserStatusRequest;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户用户管理接口权限注解契约测试。 */
class TenantUserControllerPermissionTest {

    /** 验证角色分配使用集中权限码而非控制器硬编码校验。 */
    @Test
    void shouldDeclareRoleAssignPermission() throws NoSuchMethodException {
        Method method = TenantUserController.class.getMethod(
                "replaceRoles", long.class, ReplaceTenantUserRolesRequest.class
        );
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.IAM_USER_ROLE_ASSIGN);
    }

    /** 验证不可恢复注销使用独立权限码。 */
    @Test
    void shouldDeclareCancelPermission() throws NoSuchMethodException {
        Method method = TenantUserController.class.getMethod(
                "cancel", long.class, TenantUserStatusRequest.class
        );
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.IAM_USER_CANCEL);
    }
}
