package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.audit.spi.AuditClientIpResolver;
import com.canteen.smile.modules.tenant.entity.TenantEntity;
import com.canteen.smile.modules.tenant.mapper.TenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** 平台租户生命周期本地事务命令测试。 */
@ExtendWith(MockitoExtension.class)
class PlatformTenantGovernanceCommandServiceTest {

    /** 模拟租户数据访问接口。 */
    @Mock
    private TenantMapper mapper;
    /** 模拟可信客户端 IP 解析器。 */
    @Mock
    private AuditClientIpResolver clientIpResolver;
    /** 被测试的租户治理命令服务。 */
    private PlatformTenantGovernanceCommandService service;

    /** 创建每个测试使用的命令服务。 */
    @BeforeEach
    void setUp() {
        service = new PlatformTenantGovernanceCommandService(mapper, clientIpResolver);
    }

    /** 验证暂停租户提升安全版本并在同一命令中生成账号级可靠事件。 */
    @Test
    void shouldSuspendTenantAndCreateSessionInvalidationEvents() {
        TenantEntity active = tenant("ACTIVE", 3L, 8L);
        TenantEntity suspended = tenant("SUSPENDED", 4L, 9L);
        when(mapper.selectById(10L)).thenReturn(active, suspended);
        when(mapper.changeTenantStatus(10L, List.of("ACTIVE"), "SUSPENDED", 8L, 99L))
                .thenReturn(1);
        when(mapper.selectSecurityVersion(10L)).thenReturn(4L);
        when(clientIpResolver.resolve()).thenReturn("192.168.0.64");

        TenantEntity result = service.suspend(10L, 8L, 99L);

        assertThat(result.getStatus()).isEqualTo("SUSPENDED");
        InOrder order = inOrder(mapper);
        order.verify(mapper).selectById(10L);
        order.verify(mapper).changeTenantStatus(10L, List.of("ACTIVE"), "SUSPENDED", 8L, 99L);
        order.verify(mapper).selectSecurityVersion(10L);
        order.verify(mapper).insertTenantStatusChangedEvents(
                10L, 4L, "SUSPENDED", "租户已暂停，强制全部会话失效", 99L, "192.168.0.64");
        order.verify(mapper).selectById(10L);
        verifyNoMoreInteractions(mapper);
    }

    /** @return 指定状态、安全版本和乐观锁版本的测试租户 */
    private TenantEntity tenant(String status, long securityVersion, long version) {
        TenantEntity entity = new TenantEntity();
        entity.setId(10L);
        entity.setStatus(status);
        entity.setSecurityVersion(securityVersion);
        entity.setVersion(version);
        return entity;
    }
}
