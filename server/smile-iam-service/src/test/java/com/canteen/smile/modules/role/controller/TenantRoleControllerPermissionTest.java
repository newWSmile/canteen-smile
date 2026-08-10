package com.canteen.smile.modules.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import com.canteen.smile.modules.role.dto.ReplaceRoleDataPolicyRequest;
import com.canteen.smile.modules.role.dto.ReplaceRolePermissionsRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户角色授权接口权限注解契约测试。 */
class TenantRoleControllerPermissionTest {

    /** 验证功能授权使用集中权限码。 */
    @Test
    void shouldDeclareRoleGrantPermission() throws NoSuchMethodException {
        Method method = TenantRoleController.class.getMethod(
                "replacePermissions", long.class, ReplaceRolePermissionsRequest.class
        );
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.IAM_ROLE_GRANT);
    }

    /** 验证数据范围使用独立集中权限码。 */
    @Test
    void shouldDeclareRoleDataScopePermission() throws NoSuchMethodException {
        Method method = TenantRoleController.class.getMethod(
                "replaceDataPolicies", long.class, ReplaceRoleDataPolicyRequest.class
        );
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.IAM_ROLE_DATA_SCOPE);
    }
}
