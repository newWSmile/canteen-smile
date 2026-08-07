package com.canteen.smile.modules.organization.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.canteen.smile.modules.organization.dto.CreateOrganizationRequest;
import com.canteen.smile.modules.organization.dto.MoveOrganizationRequest;
import com.canteen.smile.modules.permission.model.IamPermissionCodes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** 租户机构治理接口权限注解契约测试。 */
class TenantOrganizationControllerPermissionTest {

    /** 验证新增机构使用集中维护的权限码。 */
    @Test
    void shouldDeclareOrganizationCreatePermission() throws NoSuchMethodException {
        Method method = TenantOrganizationController.class.getMethod("create", CreateOrganizationRequest.class);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.IAM_ORG_CREATE);
    }

    /** 验证机构迁移使用独立权限码。 */
    @Test
    void shouldDeclareOrganizationMovePermission() throws NoSuchMethodException {
        Method method = TenantOrganizationController.class.getMethod(
                "move", long.class, MoveOrganizationRequest.class
        );
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.IAM_ORG_MOVE);
    }

    /** 验证类型允许关系整版替换仅由类型管理权限开放。 */
    @Test
    void shouldDeclareOrganizationTypeManagePermission() throws NoSuchMethodException {
        Method method = TenantOrganizationTypeRelationController.class.getMethod(
                "replace",
                com.canteen.smile.modules.organization.dto.ReplaceOrganizationTypeRelationsRequest.class
        );
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(IamPermissionCodes.IAM_ORG_TYPE_MANAGE);
    }
}
