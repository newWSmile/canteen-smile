package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.tenant.dto.UpdateTenantSecurityPolicyRequest;
import com.canteen.smile.modules.tenant.entity.TenantSecurityPolicyEntity;
import com.canteen.smile.modules.tenant.mapper.TenantSecurityPolicyMapper;
import com.canteen.smile.modules.tenant.vo.TenantSecurityPolicyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 租户根机构所有者查询和修改租户安全策略的应用服务。 */
@Service
@RequiredArgsConstructor
public class TenantSecurityPolicyService {

    /** 当前租户操作人解析服务。 */
    private final TenantActorService actorService;
    /** 租户安全策略数据访问接口。 */
    private final TenantSecurityPolicyMapper mapper;
    /** 安全策略本地事务命令服务。 */
    private final TenantSecurityPolicyCommandService commandService;
    /** Auth 再认证票据消费 Client。 */
    private final AuthTenantAccountClient authTenantAccountClient;

    /** @return 当前租户根机构所有者可维护的安全策略。 */
    @Transactional(readOnly = true)
    public TenantSecurityPolicyVO current() {
        TenantActorContext actor = actorService.requireRootOwner();
        return toVO(actor, requirePolicy(actor.tenantId()));
    }

    /**
     * 完成再认证后修改当前租户安全策略。
     *
     * @param request 修改命令
     * @return 修改后的安全策略
     */
    @AuditOperation(
            source = "IAM",
            categoryPath = {"租户管理端", "租户设置", "安全策略"},
            actionCode = "iam:tenant-security:manage",
            actionName = "修改租户安全策略",
            targetType = "TENANT",
            targetId = "#actor.tenantId",
            targetName = "#result?.tenantName",
            reason = "#request.reason"
    )
    public TenantSecurityPolicyVO update(UpdateTenantSecurityPolicyRequest request) {
        TenantActorContext actor = actorService.requireRootOwner();
        authTenantAccountClient.consumeTenantReauthTicket(
                actor.accountId(), request.reauthTicket(), "TENANT_SECURITY_POLICY_UPDATE"
        );
        return toVO(actor, commandService.update(actor, request));
    }

    /** 将表实体转换为接口视图。 */
    private TenantSecurityPolicyVO toVO(TenantActorContext actor, TenantSecurityPolicyEntity policy) {
        Long securityVersion = mapper.selectTenantSecurityVersion(actor.tenantId());
        if (securityVersion == null) {
            throw new IllegalStateException("Tenant security version is unavailable");
        }
        return new TenantSecurityPolicyVO(
                Long.toString(actor.tenantId()), actor.tenantName(),
                policy.getConcurrentLoginEnabled(), policy.getMaxDevices(), policy.getRememberMeEnabled(),
                policy.getIdleSeconds(), policy.getAbsoluteSeconds(), policy.getRememberIdleSeconds(),
                policy.getRememberAbsoluteSeconds(), policy.getPasswordExpiryEnabled(),
                policy.getPasswordExpiryDays(), policy.getAuditRetentionDays(), securityVersion,
                policy.getVersion()
        );
    }

    /** @return 当前租户有效策略。 */
    private TenantSecurityPolicyEntity requirePolicy(long tenantId) {
        TenantSecurityPolicyEntity policy = mapper.selectByTenantId(tenantId);
        if (policy == null) {
            throw new BusinessException("IAM_2901", "当前租户安全策略不存在", 404);
        }
        return policy;
    }
}
