package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.audit.spi.AuditClientIpResolver;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.tenant.entity.TenantEntity;
import com.canteen.smile.modules.tenant.mapper.TenantMapper;
import com.canteen.smile.modules.tenant.model.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 在 IAM 本地事务内执行租户资料和生命周期治理命令。 */
@Service
@RequiredArgsConstructor
public class PlatformTenantGovernanceCommandService {

    /** 租户数据访问接口。 */
    private final TenantMapper mapper;
    /** 当前受信任客户端 IP 解析器。 */
    private final AuditClientIpResolver clientIpResolver;

    /** 修改租户显示名称。 */
    @Transactional
    public TenantEntity updateName(long tenantId, String name, long version, long operatorId) {
        TenantEntity tenant = requireTenant(tenantId);
        if (TenantStatus.CANCELLED.name().equals(tenant.getStatus())) {
            throw new BusinessException("IAM_2212", "已注销租户不能修改资料", 409);
        }
        if (mapper.updateTenantName(tenantId, name.strip(), version, operatorId) != 1) {
            throw conflict();
        }
        return requireTenant(tenantId);
    }

    /** 暂停正常租户并使租户内全部账号会话失效。 */
    @Transactional
    public TenantEntity suspend(long tenantId, long version, long operatorId) {
        return changeStatus(tenantId, List.of(TenantStatus.ACTIVE.name()), TenantStatus.SUSPENDED,
                "租户已暂停，强制全部会话失效", version, operatorId);
    }

    /** 恢复已暂停或已到期租户；历史会话不恢复。 */
    @Transactional
    public TenantEntity resume(long tenantId, long version, long operatorId) {
        return changeStatus(tenantId,
                List.of(TenantStatus.SUSPENDED.name(), TenantStatus.EXPIRED.name()), TenantStatus.ACTIVE,
                "租户已恢复，历史会话保持失效", version, operatorId);
    }

    /** 不可恢复地注销正常、暂停或到期租户。 */
    @Transactional
    public TenantEntity cancel(long tenantId, long version, long operatorId) {
        return changeStatus(tenantId,
                List.of(TenantStatus.ACTIVE.name(), TenantStatus.SUSPENDED.name(), TenantStatus.EXPIRED.name()),
                TenantStatus.CANCELLED, "租户已注销，强制全部会话失效", version, operatorId);
    }

    /** 执行受限状态迁移并生成账号级可靠事件。 */
    private TenantEntity changeStatus(long tenantId, List<String> sourceStatuses, TenantStatus targetStatus,
                                      String actionName, long version, long operatorId) {
        requireTenant(tenantId);
        if (mapper.changeTenantStatus(
                tenantId, sourceStatuses, targetStatus.name(), version, operatorId) != 1) {
            throw conflict();
        }
        Long securityVersion = mapper.selectSecurityVersion(tenantId);
        if (securityVersion == null) {
            throw new IllegalStateException("Tenant security version is unavailable");
        }
        mapper.insertTenantStatusChangedEvents(
                tenantId, securityVersion, targetStatus.name(), actionName,
                operatorId, clientIpResolver.resolve()
        );
        return requireTenant(tenantId);
    }

    /** @return 指定有效租户。 */
    private TenantEntity requireTenant(long tenantId) {
        TenantEntity tenant = mapper.selectById(tenantId);
        if (tenant == null) {
            throw new BusinessException("IAM_2201", "租户不存在", 404);
        }
        return tenant;
    }

    /** @return 状态或版本已变化的稳定冲突异常。 */
    private BusinessException conflict() {
        return new BusinessException("IAM_2210", "租户状态或版本已变化，请刷新后重试", 409);
    }
}
