package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.tenant.converter.TenantConverter;
import com.canteen.smile.modules.tenant.dto.TenantPageQuery;
import com.canteen.smile.modules.tenant.entity.TenantEntity;
import com.canteen.smile.modules.tenant.mapper.TenantMapper;
import com.canteen.smile.modules.tenant.vo.TenantSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** 平台端租户分页查询服务测试。 */
@ExtendWith(MockitoExtension.class)
class TenantQueryServiceTest {

    /** 模拟租户 Mapper。 */
    @Mock
    private TenantMapper tenantMapper;

    /** 被测试的租户查询服务。 */
    private TenantQueryService tenantQueryService;

    /** 创建每个测试使用的查询服务。 */
    @BeforeEach
    void setUp() {
        tenantQueryService = new TenantQueryService(tenantMapper, new TenantConverter());
    }

    /** 验证状态过滤和分页偏移量直接下推 Mapper。 */
    @Test
    void shouldQueryTenantPageWithDatabasePagination() {
        /** 第三页、每页 25 条的有效租户查询。 */
        TenantPageQuery query = new TenantPageQuery();
        query.setPageNo(3);
        query.setPageSize(25);
        query.setStatus("ACTIVE");
        /** Mapper 返回的租户实体。 */
        TenantEntity entity = activeTenant();
        when(tenantMapper.countActiveTenants("ACTIVE")).thenReturn(51L);
        when(tenantMapper.selectActiveTenantPage("ACTIVE", 25, 50L)).thenReturn(List.of(entity));

        /** 服务返回的租户分页数据。 */
        PageResult<TenantSummaryVO> result = tenantQueryService.pageTenants(query);

        assertThat(result.pageNo()).isEqualTo(3);
        assertThat(result.pageSize()).isEqualTo(25);
        assertThat(result.total()).isEqualTo(51L);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo("1001");
            assertThat(item.tenantCode()).isEqualTo("TENANT-001");
        });
        verify(tenantMapper).countActiveTenants("ACTIVE");
        verify(tenantMapper).selectActiveTenantPage("ACTIVE", 25, 50L);
    }

    /** 验证空结果不会额外执行分页数据查询。 */
    @Test
    void shouldSkipPageSelectWhenNoTenantMatches() {
        /** 使用默认分页值且不限制状态的查询。 */
        TenantPageQuery query = new TenantPageQuery();
        when(tenantMapper.countActiveTenants(null)).thenReturn(0L);

        /** 服务返回的空分页数据。 */
        PageResult<TenantSummaryVO> result = tenantQueryService.pageTenants(query);

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
        verify(tenantMapper).countActiveTenants(null);
        verifyNoMoreInteractions(tenantMapper);
    }

    /** @return 测试使用的正常租户实体 */
    private TenantEntity activeTenant() {
        /** 正常状态租户实体。 */
        TenantEntity entity = new TenantEntity();
        entity.setId(1001L);
        entity.setTenantCode("TENANT-001");
        entity.setName("测试租户");
        entity.setStatus("ACTIVE");
        entity.setRootOrganizationId(2001L);
        entity.setSecurityVersion(1L);
        entity.setTemplateVersion(1L);
        entity.setProvisionStatus("ACTIVE");
        entity.setCreatedTime(OffsetDateTime.parse("2026-08-06T10:00:00+08:00"));
        entity.setVersion(0L);
        return entity;
    }
}
