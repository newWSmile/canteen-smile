package com.canteen.smile.modules.organization.service;

import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.organization.dto.TransferOrganizationOwnerRequest;
import com.canteen.smile.modules.organization.mapper.OrganizationOwnerMapper;
import com.canteen.smile.modules.organization.vo.OrganizationOwnerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 机构所有权查询与跨服务编排。 */
@Service
@RequiredArgsConstructor
public class OrganizationOwnerService {
    /** 当前身份服务。 */
    private final TenantActorService actorService;
    /** 所有权数据访问接口。 */
    private final OrganizationOwnerMapper mapper;
    /** 所有权事务服务。 */
    private final OrganizationOwnerCommandService commandService;
    /** Auth 内部 Client。 */
    private final AuthTenantAccountClient authClient;

    /** @return 当前机构所有者 */
    @Transactional(readOnly = true)
    public OrganizationOwnerVO current() {
        TenantActorContext actor = actorService.current();
        return toVO(mapper.selectOwner(actor.tenantId(), actor.organizationId()));
    }

    /** @param request 转让请求 @return 新所有者 */
    public OrganizationOwnerVO transfer(TransferOrganizationOwnerRequest request) {
        TenantActorContext actor = actorService.current();
        authClient.consumeTenantReauthTicket(actor.accountId(), request.reauthTicket(),
                "TENANT_ORG_OWNER_TRANSFER");
        commandService.transfer(actor, request);
        return current();
    }

    /** @return 对外所有者摘要 */
    private OrganizationOwnerVO toVO(OrganizationOwnerMapper.OwnerRow row) {
        if (row == null) {
            throw new BusinessException("IAM_2616", "当前机构尚未配置所有者", 404);
        }
        return new OrganizationOwnerVO(Long.toString(row.organizationId()), Long.toString(row.accountId()),
                row.username(), row.displayName(), row.effectiveTime(), row.version());
    }
}
