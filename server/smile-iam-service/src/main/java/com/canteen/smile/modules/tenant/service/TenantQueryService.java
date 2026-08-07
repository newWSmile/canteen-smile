package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.modules.tenant.converter.TenantConverter;
import com.canteen.smile.modules.tenant.dto.TenantPageQuery;
import com.canteen.smile.modules.tenant.entity.TenantEntity;
import com.canteen.smile.modules.tenant.mapper.TenantMapper;
import com.canteen.smile.modules.tenant.vo.TenantSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 平台端租户只读查询服务。 */
@Service
@RequiredArgsConstructor
public class TenantQueryService {

    /** 租户数据访问接口。 */
    private final TenantMapper tenantMapper;

    /** 租户对象转换器。 */
    private final TenantConverter tenantConverter;

    /**
     * 分页查询平台可见的有效租户。
     *
     * @param query 已通过 Validation 校验的分页条件
     * @return 租户分页摘要
     */
    @Transactional(readOnly = true)
    public PageResult<TenantSummaryVO> pageTenants(TenantPageQuery query) {
        /** 满足过滤条件的租户总量。 */
        long total = tenantMapper.countActiveTenants(query.getStatus());
        if (total == 0) {
            return new PageResult<>(List.of(), query.getPageNo(), query.getPageSize(), 0);
        }

        /** PostgreSQL 分页偏移量，使用 long 避免大页码整数溢出。 */
        long offset = (long) (query.getPageNo() - 1) * query.getPageSize();
        /** 当前页数据库实体。 */
        List<TenantEntity> entities = tenantMapper.selectActiveTenantPage(
                query.getStatus(),
                query.getPageSize(),
                offset
        );
        /** 当前页对外租户摘要。 */
        List<TenantSummaryVO> items = entities.stream().map(tenantConverter::toSummary).toList();
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), total);
    }
}
