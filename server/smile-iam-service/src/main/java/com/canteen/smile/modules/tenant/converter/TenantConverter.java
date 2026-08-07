package com.canteen.smile.modules.tenant.converter;

import com.canteen.smile.modules.tenant.entity.TenantEntity;
import com.canteen.smile.modules.tenant.model.TenantProvisionStatus;
import com.canteen.smile.modules.tenant.model.TenantStatus;
import com.canteen.smile.modules.tenant.vo.TenantSummaryVO;
import org.springframework.stereotype.Component;

/** 租户 Entity 与对外 VO 的显式转换器。 */
@Component
public class TenantConverter {

    /**
     * 将租户数据库实体转换为平台端分页摘要。
     *
     * @param entity 租户数据库实体
     * @return 租户分页摘要
     */
    public TenantSummaryVO toSummary(TenantEntity entity) {
        return new TenantSummaryVO(
                entity.getId().toString(),
                entity.getTenantCode(),
                entity.getName(),
                TenantStatus.valueOf(entity.getStatus()),
                entity.getRootOrganizationId() == null ? null : entity.getRootOrganizationId().toString(),
                entity.getSecurityVersion(),
                entity.getTemplateVersion(),
                TenantProvisionStatus.valueOf(entity.getProvisionStatus()),
                entity.getCreatedTime(),
                entity.getVersion()
        );
    }
}
