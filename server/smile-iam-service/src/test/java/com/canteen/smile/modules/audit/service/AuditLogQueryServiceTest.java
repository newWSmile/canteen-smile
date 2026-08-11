package com.canteen.smile.modules.audit.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.internal.client.AuthAuditLogClient;
import com.canteen.smile.internal.client.dto.AuthAuditLogInternalResponse;
import com.canteen.smile.internal.client.dto.AuthAuditLogSearchInternalRequest;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.audit.dto.AuditLogPageQuery;
import com.canteen.smile.modules.audit.mapper.IamAuditLogMapper;
import com.canteen.smile.modules.platform.service.PlatformActorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** IAM 审计聚合查询的数据边界测试。 */
@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    /** 模拟 IAM 审计 Mapper。 */
    @Mock private IamAuditLogMapper mapper;

    /** 模拟 Auth 审计 Client。 */
    @Mock private AuthAuditLogClient authClient;

    /** 模拟平台身份服务。 */
    @Mock private PlatformActorService platformActorService;

    /** 模拟租户身份服务。 */
    @Mock private TenantActorService tenantActorService;

    /** 被测试的审计聚合查询服务。 */
    private AuditLogQueryService service;

    /** 创建每个测试使用的服务。 */
    @BeforeEach
    void setUp() {
        service = new AuditLogQueryService(
                mapper, authClient, platformActorService, tenantActorService, new ObjectMapper()
        );
    }

    /** 验证普通管理员的 IAM 查询将租户、机构和本人边界全部下推 Mapper。 */
    @Test
    void shouldPushOrdinaryTenantIamScopeToDatabase() {
        /** 普通租户管理员身份。 */
        TenantActorContext actor = actor(false);
        /** 默认 IAM 来源查询。 */
        AuditLogPageQuery query = new AuditLogPageQuery();
        when(tenantActorService.current()).thenReturn(actor);
        when(mapper.countPage(false, 2L, 20L, 200L, false,
                null, null, null, null, null)).thenReturn(0L);

        service.pageTenant(query);

        verify(mapper).countPage(false, 2L, 20L, 200L, false,
                null, null, null, null, null);
        verify(authClient, never()).page(org.mockito.ArgumentMatchers.any());
    }

    /** 验证租户所有者查询 Auth 时只能发送当前租户全部作用域。 */
    @Test
    void shouldSendRootOwnerTenantBoundaryToAuth() {
        /** 租户根机构所有者身份。 */
        TenantActorContext actor = actor(true);
        /** Auth 来源查询。 */
        AuditLogPageQuery query = new AuditLogPageQuery();
        query.setSource("AUTH");
        when(tenantActorService.current()).thenReturn(actor);
        when(authClient.page(org.mockito.ArgumentMatchers.any())).thenReturn(
                new PageResult<AuthAuditLogInternalResponse>(List.of(), 1, 20, 0)
        );
        /** Auth 内部请求捕获器。 */
        ArgumentCaptor<AuthAuditLogSearchInternalRequest> captor =
                ArgumentCaptor.forClass(AuthAuditLogSearchInternalRequest.class);

        service.pageTenant(query);

        verify(authClient).page(captor.capture());
        assertThat(captor.getValue()).satisfies(request -> {
            assertThat(request.scopeType()).isEqualTo("TENANT");
            assertThat(request.tenantId()).isEqualTo(2L);
            assertThat(request.accountId()).isEqualTo(200L);
            assertThat(request.tenantWide()).isTrue();
        });
        verify(mapper, never()).countPage(
                org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
    }

    /** 验证认证审计响应会补充中文语义，并保留原始编码与身份快照。 */
    @Test
    void shouldMapAuthAuditToChinesePresentationFields() {
        TenantActorContext actor = actor(true);
        AuditLogPageQuery query = new AuditLogPageQuery();
        query.setSource("AUTH");
        OffsetDateTime occurredTime = OffsetDateTime.now();
        AuthAuditLogInternalResponse row = new AuthAuditLogInternalResponse(
                "101", "2", "TENANT_ACCOUNT", "9", "audit_user", "审计用户",
                "TENANT_ACCOUNT", "9", "audit_user", "审计用户",
                "auth:login:password", "用户名密码登录", "SUCCESS", "PASSWORD", null,
                "138****0000", "Web / Chrome", "trace-1", occurredTime,
                "TENANT_ADMIN", List.of("租户端", "账号安全"),
                null, null, null, null, null, 12L
        );
        when(tenantActorService.current()).thenReturn(actor);
        when(authClient.page(org.mockito.ArgumentMatchers.any())).thenReturn(
                new PageResult<>(List.of(row), 1, 20, 1)
        );

        PageResult<com.canteen.smile.modules.audit.vo.AuditLogVO> result = service.pageTenant(query);

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.actionName()).isEqualTo("用户名密码登录");
            assertThat(item.operatorTypeName()).isEqualTo("租户账号");
            assertThat(item.operatorUsername()).isEqualTo("audit_user");
            assertThat(item.operatorDisplayName()).isEqualTo("审计用户");
            assertThat(item.targetTypeName()).isEqualTo("租户账号");
            assertThat(item.targetName()).isEqualTo("审计用户");
            assertThat(item.targetCode()).isEqualTo("audit_user");
            assertThat(item.loginMethodName()).isEqualTo("用户名密码");
            assertThat(item.actionCode()).isEqualTo("auth:login:password");
            assertThat(item.traceId()).isEqualTo("trace-1");
            assertThat(item.appCode()).isEqualTo("TENANT_ADMIN");
            assertThat(item.categoryPath()).containsExactly("租户端", "账号安全");
            assertThat(item.durationMs()).isEqualTo(12L);
        });
    }

    /** @param rootOwner 是否根机构所有者 @return 测试租户身份 */
    private TenantActorContext actor(boolean rootOwner) {
        return new TenantActorContext(
                200L, 2L, "测试租户", 20L, "测试机构", 20L,
                "audit_admin", "审计管理员", rootOwner
        );
    }
}
