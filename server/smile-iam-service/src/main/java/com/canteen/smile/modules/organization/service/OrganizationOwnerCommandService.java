package com.canteen.smile.modules.organization.service;

import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.audit.spi.AuditClientIpResolver;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.mapper.TenantUserMapper;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.organization.dto.TransferOrganizationOwnerRequest;
import com.canteen.smile.modules.organization.mapper.OrganizationOwnerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 机构所有权转让本地事务服务。 */
@Service
@RequiredArgsConstructor
public class OrganizationOwnerCommandService {

    /** 所有权数据访问接口。 */
    private final OrganizationOwnerMapper mapper;
    /** 用户数据访问接口。 */
    private final TenantUserMapper userMapper;
    /** 当前请求客户端 IP 解析器。 */
    private final AuditClientIpResolver clientIpResolver;

    /**
     * 原子转让当前机构所有权。
     *
     * @param actor 当前所有者
     * @param request 转让请求
     * @return 转让后的所有者账号 ID
     */
    @Transactional
    @AuditOperation(
            source = "IAM", categoryPath = {"租户端", "机构治理", "机构所有权"},
            actionCode = "iam:org-owner:transfer", actionName = "转让机构所有权",
            targetType = "ORGANIZATION", targetId = "#actor.organizationId",
            targetName = "#actor.organizationName", reason = "#request.reason"
    )
    public long transfer(TenantActorContext actor, TransferOrganizationOwnerRequest request) {
        OrganizationOwnerMapper.OwnerRow owner = requireOwner(actor);
        long targetAccountId = Long.parseLong(request.targetAccountId());
        if (targetAccountId == actor.accountId()) {
            throw new BusinessException("IAM_2611", "新所有者不能是当前所有者", 400);
        }
        TenantUserMapper.UserRow target = userMapper.selectUser(
                actor.tenantId(), actor.organizationId(), targetAccountId
        );
        if (target == null || !"ACTIVE".equals(target.status()) || target.owner()) {
            throw new BusinessException("IAM_2612", "新所有者必须是本机构正常账号", 409);
        }
        if (userMapper.countCustomRoles(actor.tenantId(), actor.organizationId(), actor.accountId()) == 0) {
            throw new BusinessException("IAM_2613", "当前所有者转让前至少需要保留一个自定义角色", 409);
        }
        if (userMapper.countCustomRoles(actor.tenantId(), actor.organizationId(), targetAccountId) == 0) {
            throw new BusinessException("IAM_2614", "新所有者必须先拥有至少一个自定义角色", 409);
        }
        if (mapper.deactivateOwnerRole(actor.tenantId(), actor.organizationId(), actor.accountId(),
                owner.protectedRoleId(), actor.accountId()) != 1) {
            throw concurrentChange();
        }
        mapper.insertOwnerRole(actor.tenantId(), actor.organizationId(), targetAccountId,
                owner.protectedRoleId(), actor.accountId());
        if (mapper.transferOwner(actor.tenantId(), actor.organizationId(), actor.accountId(), targetAccountId,
                request.version(), actor.accountId()) != 1) {
            throw concurrentChange();
        }
        mapper.insertHistory(actor.tenantId(), actor.organizationId(), actor.accountId(), targetAccountId,
                request.reason().strip(), actor.accountId());
        mapper.bumpAccountVersion(actor.tenantId(), actor.organizationId(), actor.accountId(), actor.accountId());
        mapper.bumpAccountVersion(actor.tenantId(), actor.organizationId(), targetAccountId, actor.accountId());
        userMapper.insertAccountChangedOutbox(userMapper.nextOutboxId(), java.util.UUID.randomUUID().toString(),
                actor.tenantId(), actor.accountId(), "SESSION_INVALIDATION_REQUESTED",
                "机构所有权已转让，强制会话失效", actor.accountId(), clientIpResolver.resolve());
        userMapper.insertAccountChangedOutbox(userMapper.nextOutboxId(), java.util.UUID.randomUUID().toString(),
                actor.tenantId(), targetAccountId, "SESSION_INVALIDATION_REQUESTED",
                "机构所有权已转让，强制会话失效", actor.accountId(), clientIpResolver.resolve());
        return targetAccountId;
    }

    /** @return 当前账号确为本机构所有者时的关系 */
    private OrganizationOwnerMapper.OwnerRow requireOwner(TenantActorContext actor) {
        OrganizationOwnerMapper.OwnerRow owner = mapper.selectOwner(actor.tenantId(), actor.organizationId());
        if (owner == null || owner.accountId() != actor.accountId()) {
            throw new BusinessException("IAM_2610", "只有当前机构所有者可以转让所有权", 403);
        }
        return owner;
    }

    /** @return 所有权并发变化异常 */
    private BusinessException concurrentChange() {
        return new BusinessException("IAM_2615", "机构所有权已变化，请刷新后重试", 409);
    }
}
