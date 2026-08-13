package com.canteen.smile.modules.organization.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.audit.service.IamAuditLogService;
import com.canteen.smile.modules.organization.dto.MoveOrganizationRequest;
import com.canteen.smile.modules.organization.mapper.TenantOrganizationMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 租户机构树结构校验测试。 */
class TenantOrganizationServiceTest {

    /** 验证不能把机构迁移到自身的后代节点下。 */
    @Test
    void shouldRejectMovingOrganizationBelowDescendant() {
        /** 当前租户操作人服务替身。 */
        TenantActorService actorService = mock(TenantActorService.class);
        /** 机构 Mapper 替身。 */
        TenantOrganizationMapper mapper = mock(TenantOrganizationMapper.class);
        /** 审计服务替身。 */
        IamAuditLogService audit = mock(IamAuditLogService.class);
        when(actorService.current()).thenReturn(new TenantActorContext(
                21L, 11L, "测试租户", 31L, "根机构", 31L, "owner", "Owner", true, true
        ));
        when(mapper.selectOrganization(11L, 41L)).thenReturn(organization(41L, 31L, 1L));
        when(mapper.selectOrganization(11L, 51L)).thenReturn(organization(51L, 41L, 2L));
        when(mapper.countOrganizationPath(11L, 41L, 51L)).thenReturn(1L);
        /** 被测服务。 */
        TenantOrganizationService service = new TenantOrganizationService(actorService, mapper, audit);
        /** 指向后代节点的迁移请求。 */
        MoveOrganizationRequest request = new MoveOrganizationRequest(51L, 0L, "调整机构结构");

        assertThatThrownBy(() -> service.move(41L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("新父机构不能是当前机构或其下级机构");
        verify(mapper, never()).moveOrganization(11L, 41L, 51L, 0L, 21L);
    }

    /** @return 测试机构行 */
    private TenantOrganizationMapper.OrganizationRow organization(
            long id,
            Long parentId,
            long typeId
    ) {
        return new TenantOrganizationMapper.OrganizationRow(
                id, parentId, typeId, "TYPE", "类型", "ORG_" + id, "机构" + id,
                null, "ACTIVE", "ACTIVE", 0L, false, 0L
        );
    }
}
