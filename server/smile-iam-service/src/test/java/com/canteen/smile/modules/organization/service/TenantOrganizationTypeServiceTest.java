package com.canteen.smile.modules.organization.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.audit.service.IamAuditLogService;
import com.canteen.smile.modules.organization.dto.OrganizationTypeRelationRequest;
import com.canteen.smile.modules.organization.dto.ReplaceOrganizationTypeRelationsRequest;
import com.canteen.smile.modules.organization.mapper.TenantOrganizationMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 租户机构类型允许关系整图校验测试。 */
class TenantOrganizationTypeServiceTest {

    /** 验证租户关系图形成环时在写库前被拒绝。 */
    @Test
    void shouldRejectCyclicTenantRelations() {
        /** 当前租户操作人服务替身。 */
        TenantActorService actorService = mock(TenantActorService.class);
        /** 机构 Mapper 替身。 */
        TenantOrganizationMapper mapper = mock(TenantOrganizationMapper.class);
        /** 审计服务替身。 */
        IamAuditLogService audit = mock(IamAuditLogService.class);
        when(actorService.requireRootOwner()).thenReturn(actor());
        when(mapper.selectActiveOrganizationTypes(11L)).thenReturn(types());
        /** 被测服务。 */
        TenantOrganizationTypeService service = new TenantOrganizationTypeService(actorService, mapper, audit);
        /** CITY 与 SCHOOL 构成环的完整关系请求。 */
        ReplaceOrganizationTypeRelationsRequest request = new ReplaceOrganizationTypeRelationsRequest(List.of(
                new OrganizationTypeRelationRequest(1L, 2L),
                new OrganizationTypeRelationRequest(2L, 1L)
        ));

        assertThatThrownBy(() -> service.replaceRelations(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("机构类型允许关系不能形成环");
        verify(mapper, never()).deactivateOrganizationTypeRelations(11L, 21L);
    }

    /** 验证不能从完整关系集合中移除真实机构树正在使用的边。 */
    @Test
    void shouldKeepRelationsUsedByOrganizationTree() {
        /** 当前租户操作人服务替身。 */
        TenantActorService actorService = mock(TenantActorService.class);
        /** 机构 Mapper 替身。 */
        TenantOrganizationMapper mapper = mock(TenantOrganizationMapper.class);
        /** 审计服务替身。 */
        IamAuditLogService audit = mock(IamAuditLogService.class);
        when(actorService.requireRootOwner()).thenReturn(actor());
        when(mapper.selectActiveOrganizationTypes(11L)).thenReturn(types());
        when(mapper.selectUsedOrganizationTypeRelations(11L)).thenReturn(List.of(
                new TenantOrganizationMapper.OrganizationTypePairRow(1L, 2L)
        ));
        /** 被测服务。 */
        TenantOrganizationTypeService service = new TenantOrganizationTypeService(actorService, mapper, audit);
        /** 未保留任何边的完整关系请求。 */
        ReplaceOrganizationTypeRelationsRequest request = new ReplaceOrganizationTypeRelationsRequest(List.of());

        assertThatThrownBy(() -> service.replaceRelations(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能删除机构树正在使用的类型父子关系");
        verify(mapper, never()).deactivateOrganizationTypeRelations(11L, 21L);
    }

    /** @return 根机构所有者测试上下文 */
    private TenantActorContext actor() {
        return new TenantActorContext(21L, 11L, "测试租户", 31L, "根机构", 31L, "owner", "Owner", true);
    }

    /** @return CITY 和 SCHOOL 两个有效机构类型 */
    private List<TenantOrganizationMapper.OrganizationTypeRow> types() {
        return List.of(
                new TenantOrganizationMapper.OrganizationTypeRow(1L, "CITY", "市", 10, "ACTIVE", 1L, 0L),
                new TenantOrganizationMapper.OrganizationTypeRow(2L, "SCHOOL", "学校", 20, "ACTIVE", 1L, 0L)
        );
    }
}
