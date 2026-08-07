package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.internal.client.dto.TenantActivationTicketInternalResponse;
import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import com.canteen.smile.modules.tenant.vo.TenantOwnerActivationLinkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 平台为租户根机构首位所有者生成一次性激活信息的服务。 */
@Service
@RequiredArgsConstructor
public class TenantOwnerActivationLinkService {

    /** 所有者当前不可激活错误码。 */
    private static final String OWNER_NOT_ACTIVATABLE_CODE = "IAM_2303";

    /** 账号生命周期数据访问接口。 */
    private final AccountLifecycleMapper mapper;

    /** Auth 租户账号 Client。 */
    private final AuthTenantAccountClient authClient;

    /**
     * 校验真实所有者状态后请求 Auth 签发票据；跨服务调用不放在数据库事务中。
     *
     * @param tenantId 租户 ID
     * @return 只展示一次的激活信息
     */
    public TenantOwnerActivationLinkVO issue(long tenantId) {
        AccountLifecycleMapper.ActivationContextRow owner = findOwner(tenantId);
        TenantActivationTicketInternalResponse ticket = authClient.issueActivationTicket(owner.accountId());
        return new TenantOwnerActivationLinkVO(
                Long.toString(owner.tenantId()),
                Long.toString(owner.accountId()),
                ticket.activationTicket(),
                ticket.expiresAt()
        );
    }

    /** @param tenantId 租户 ID @return 可激活根机构所有者 */
    @Transactional(readOnly = true)
    public AccountLifecycleMapper.ActivationContextRow findOwner(long tenantId) {
        AccountLifecycleMapper.ActivationContextRow owner = mapper.selectRootOwnerActivationContext(tenantId);
        if (owner == null || !"ACTIVE".equals(owner.tenantStatus())
                || !"PENDING_ACTIVATION".equals(owner.accountStatus())) {
            throw new BusinessException(OWNER_NOT_ACTIVATABLE_CODE, "租户所有者不存在或已经完成激活", 409);
        }
        return owner;
    }
}
