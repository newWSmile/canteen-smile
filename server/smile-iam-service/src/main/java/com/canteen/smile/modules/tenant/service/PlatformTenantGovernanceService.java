package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import com.canteen.smile.modules.account.model.AccountStatus;
import com.canteen.smile.modules.platform.service.PlatformActorService;
import com.canteen.smile.modules.tenant.converter.TenantConverter;
import com.canteen.smile.modules.tenant.dto.PlatformTenantStatusRequest;
import com.canteen.smile.modules.tenant.dto.UpdatePlatformTenantRequest;
import com.canteen.smile.modules.tenant.entity.TenantEntity;
import com.canteen.smile.modules.tenant.vo.TenantSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 平台租户资料与生命周期治理应用服务。 */
@Service
@RequiredArgsConstructor
public class PlatformTenantGovernanceService {

    /** 当前平台操作人解析服务。 */
    private final PlatformActorService actorService;
    /** Auth 再认证票据消费 Client。 */
    private final AuthTenantAccountClient authClient;
    /** 本地事务命令服务。 */
    private final PlatformTenantGovernanceCommandService commandService;
    /** 租户根机构所有者查询接口。 */
    private final AccountLifecycleMapper accountLifecycleMapper;
    /** 租户对象显式转换器。 */
    private final TenantConverter converter;

    /** 修改租户名称。 */
    @AuditOperation(
            source = "IAM", categoryPath = {"平台管理端", "租户治理", "租户资料"},
            actionCode = "platform:tenant:update", actionName = "修改租户资料",
            targetType = "TENANT", targetId = "#tenantId", targetName = "#result?.name"
    )
    public TenantSummaryVO update(long tenantId, UpdatePlatformTenantRequest request) {
        long operatorId = actorService.currentPlatformIdentityId();
        return toSummary(commandService.updateName(tenantId, request.name(), request.version(), operatorId));
    }

    /** 暂停租户。 */
    @AuditOperation(
            source = "IAM", categoryPath = {"平台管理端", "租户治理", "生命周期"},
            actionCode = "platform:tenant:suspend", actionName = "暂停租户",
            targetType = "TENANT", targetId = "#tenantId", targetName = "#result?.name",
            reason = "#request.reason"
    )
    public TenantSummaryVO suspend(long tenantId, PlatformTenantStatusRequest request) {
        long operatorId = reauthenticate(request);
        return toSummary(commandService.suspend(tenantId, request.version(), operatorId));
    }

    /** 恢复租户。 */
    @AuditOperation(
            source = "IAM", categoryPath = {"平台管理端", "租户治理", "生命周期"},
            actionCode = "platform:tenant:resume", actionName = "恢复租户",
            targetType = "TENANT", targetId = "#tenantId", targetName = "#result?.name",
            reason = "#request.reason"
    )
    public TenantSummaryVO resume(long tenantId, PlatformTenantStatusRequest request) {
        long operatorId = reauthenticate(request);
        return toSummary(commandService.resume(tenantId, request.version(), operatorId));
    }

    /** 不可恢复地注销租户。 */
    @AuditOperation(
            source = "IAM", categoryPath = {"平台管理端", "租户治理", "生命周期"},
            actionCode = "platform:tenant:cancel", actionName = "注销租户",
            targetType = "TENANT", targetId = "#tenantId", targetName = "#result?.name",
            reason = "#request.reason"
    )
    public TenantSummaryVO cancel(long tenantId, PlatformTenantStatusRequest request) {
        long operatorId = reauthenticate(request);
        return toSummary(commandService.cancel(tenantId, request.version(), operatorId));
    }

    /** 消费仅允许平台租户治理动作使用的一次性再认证票据。 */
    private long reauthenticate(PlatformTenantStatusRequest request) {
        long operatorId = actorService.currentPlatformIdentityId();
        authClient.consumePlatformReauthTicket(
                operatorId, request.reauthTicket(), "PLATFORM_TENANT_GOVERNANCE"
        );
        return operatorId;
    }

    /** 把更新后的租户与所有者摘要组装为稳定响应。 */
    private TenantSummaryVO toSummary(TenantEntity tenant) {
        AccountLifecycleMapper.TenantOwnerSummaryRow owner = accountLifecycleMapper
                .selectRootOwnerSummaries(java.util.List.of(tenant.getId()))
                .stream().findFirst().orElse(null);
        return converter.toSummary(
                tenant,
                owner == null ? null : owner.username(),
                owner == null ? null : AccountStatus.valueOf(owner.accountStatus())
        );
    }
}
