package com.canteen.smile.modules.tenant.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.internal.client.dto.TenantPasswordResetTicketInternalResponse;
import com.canteen.smile.modules.account.mapper.AccountLifecycleMapper;
import com.canteen.smile.modules.account.service.AccountPasswordResetService;
import com.canteen.smile.modules.audit.service.IamAuditLogService;
import com.canteen.smile.modules.platform.service.PlatformActorService;
import com.canteen.smile.modules.tenant.dto.TenantOwnerPasswordResetRequest;
import com.canteen.smile.modules.tenant.vo.TenantOwnerPasswordResetLinkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 平台为租户根机构所有者生成一次性密码恢复链接的编排服务。 */
@Service
@RequiredArgsConstructor
public class TenantOwnerPasswordResetLinkService {

    /** 所有者当前不可恢复密码错误码。 */
    private static final String OWNER_NOT_RESETTABLE_CODE = "IAM_2307";

    /** 账号生命周期数据访问接口。 */
    private final AccountLifecycleMapper mapper;

    /** IAM 账号密码恢复状态事务服务。 */
    private final AccountPasswordResetService accountPasswordResetService;

    /** Auth 租户账号 Client。 */
    private final AuthTenantAccountClient authClient;

    /** 当前平台操作人解析服务。 */
    private final PlatformActorService platformActorService;

    /** IAM 敏感操作审计服务。 */
    private final IamAuditLogService auditLogService;

    /**
     * 校验所有者边界、设置待重置状态并请求 Auth 签发一次性票据。
     *
     * @param tenantId 租户 ID
     * @param request 再认证票据和操作原因
     * @return 只展示一次的密码恢复信息
     */
    public TenantOwnerPasswordResetLinkVO issue(
            long tenantId,
            TenantOwnerPasswordResetRequest request
    ) {
        AccountLifecycleMapper.ActivationContextRow owner = findOwner(tenantId);
        /** 当前平台身份 ID。 */
        long actorId = platformActorService.currentPlatformIdentityId();
        try {
            TenantPasswordResetTicketInternalResponse ticket = authClient.issuePasswordResetTicket(
                    owner.accountId(),
                    actorId,
                    request.reauthTicket()
            );
            accountPasswordResetService.requirePasswordReset(owner.accountId(), actorId);
            auditLogService.recordPlatformAccountAction(
                    owner.tenantId(),
                    actorId,
                    "TENANT_OWNER_PASSWORD_RESET",
                    "签发租户所有者密码恢复链接",
                    owner.accountId(),
                    request.reason(),
                    "SUCCESS"
            );
            return new TenantOwnerPasswordResetLinkVO(
                    Long.toString(owner.tenantId()),
                    Long.toString(owner.accountId()),
                    ticket.resetTicket(),
                    ticket.expiresAt()
            );
        } catch (RuntimeException exception) {
            auditLogService.recordPlatformAccountAction(
                    owner.tenantId(),
                    actorId,
                    "TENANT_OWNER_PASSWORD_RESET",
                    "签发租户所有者密码恢复链接",
                    owner.accountId(),
                    request.reason(),
                    "FAILURE"
            );
            throw exception;
        }
    }

    /** @param tenantId 租户 ID @return 可恢复密码的根机构所有者 */
    @Transactional(readOnly = true)
    public AccountLifecycleMapper.ActivationContextRow findOwner(long tenantId) {
        AccountLifecycleMapper.ActivationContextRow owner = mapper.selectRootOwnerActivationContext(tenantId);
        if (owner == null || !"ACTIVE".equals(owner.tenantStatus())
                || !("ACTIVE".equals(owner.accountStatus())
                || "PASSWORD_RESET_REQUIRED".equals(owner.accountStatus()))) {
            throw new BusinessException(
                    OWNER_NOT_RESETTABLE_CODE,
                    "租户所有者不存在、尚未激活或当前不可恢复密码",
                    409
            );
        }
        return owner;
    }
}
