package com.canteen.smile.modules.navigation.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.navigation.dto.UpdateTenantMenuVisibilityRequest;
import com.canteen.smile.modules.navigation.mapper.TenantNavigationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 租户导航恢复入口防锁死规则测试。 */
@ExtendWith(MockitoExtension.class)
class TenantNavigationServiceTest {

    /** 当前操作人服务。 */
    @Mock
    private TenantActorService actorService;
    /** 导航数据访问接口。 */
    @Mock
    private TenantNavigationMapper mapper;
    /** 导航命令服务。 */
    @Mock
    private TenantNavigationCommandService commandService;
    /** Auth 再认证客户端。 */
    @Mock
    private AuthTenantAccountClient authClient;
    /** 被测导航服务。 */
    private TenantNavigationService service;

    /** 初始化被测服务和当前操作人。 */
    @BeforeEach
    void setUp() {
        service = new TenantNavigationService(actorService, mapper, commandService, authClient);
        when(actorService.current()).thenReturn(new TenantActorContext(
                21L, 11L, "测试租户", 31L, "测试机构", 31L,
                "tenant_admin", "租户管理员", false, false
        ));
    }

    /** 租户统一配置不能隐藏功能与菜单治理恢复入口。 */
    @Test
    void shouldRejectTenantHidingNavigationRecoveryEntry() {
        when(mapper.selectMenus(11L, 21L)).thenReturn(List.of(recoveryMenu()));

        assertThatThrownBy(() -> service.updateTenantMenu("iam:tenant-navigation:view",
                new UpdateTenantMenuVisibilityRequest(true, 0L, "reauth-ticket", "测试隐藏")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("功能与菜单是租户治理恢复入口，不能隐藏");

        verifyNoInteractions(authClient, commandService);
    }

    /** @return 功能与菜单恢复入口数据行。 */
    private TenantNavigationMapper.MenuRow recoveryMenu() {
        return new TenantNavigationMapper.MenuRow(
                101L, null, "iam:tenant-navigation:view", "功能与菜单", "/tenant/navigation",
                null, true, false, 0L, false, null, 900
        );
    }
}
