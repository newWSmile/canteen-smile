package com.canteen.smile.modules.account.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.audit.annotation.AuditOperation;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.internal.client.AuthTenantAccountClient;
import com.canteen.smile.internal.client.dto.TenantActivationTicketInternalResponse;
import com.canteen.smile.modules.account.dto.CreateTenantUserRequest;
import com.canteen.smile.modules.account.dto.ReplaceTenantUserRolesRequest;
import com.canteen.smile.modules.account.dto.TenantUserPageQuery;
import com.canteen.smile.modules.account.dto.TenantUserStatusRequest;
import com.canteen.smile.modules.account.dto.TenantUserPasswordResetRequest;
import com.canteen.smile.modules.account.dto.UpdateTenantUserRequest;
import com.canteen.smile.modules.account.mapper.TenantUserMapper;
import com.canteen.smile.modules.account.vo.TenantUserActivationLinkVO;
import com.canteen.smile.modules.account.vo.TenantUserRoleVO;
import com.canteen.smile.modules.account.vo.TenantUserVO;
import com.canteen.smile.modules.account.vo.TenantUserPasswordResetLinkVO;
import com.canteen.smile.internal.client.dto.TenantPasswordResetTicketInternalResponse;
import com.canteen.smile.modules.platform.service.UsernameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 本机构用户查询和跨 Auth 编排服务。 */
@Service
@RequiredArgsConstructor
public class TenantUserService {

    /** 用户数据访问接口。 */
    private final TenantUserMapper mapper;
    /** 当前租户操作者服务。 */
    private final TenantActorService actorService;
    /** 用户本地事务服务。 */
    private final TenantUserCommandService commandService;
    /** IAM → Auth 账号 Client。 */
    private final AuthTenantAccountClient authClient;
    /** 密码重置状态服务。 */
    private final AccountPasswordResetService passwordResetService;

    /** @param query 查询条件 @return 本机构用户分页 */
    @Transactional(readOnly = true)
    public PageResult<TenantUserVO> page(TenantUserPageQuery query) {
        TenantActorContext actor = actorService.current();
        String keyword = query.getKeyword() == null || query.getKeyword().isBlank()
                ? null : UsernameNormalizer.normalize(query.getKeyword());
        long total = mapper.countUsers(actor.tenantId(), actor.organizationId(), keyword, query.getStatus());
        List<TenantUserMapper.UserRow> rows = mapper.selectUsers(actor.tenantId(), actor.organizationId(), keyword,
                query.getStatus(), (query.getPageNo() - 1) * query.getPageSize(), query.getPageSize());
        return new PageResult<>(toVOs(actor, rows), query.getPageNo(), query.getPageSize(), total);
    }

    /** @param accountId 账号 ID @return 本机构用户详情 */
    @Transactional(readOnly = true)
    public TenantUserVO detail(long accountId) {
        TenantActorContext actor = actorService.current();
        TenantUserMapper.UserRow row = requireUser(actor, accountId);
        return toVOs(actor, List.of(row)).get(0);
    }

    /** @param request 创建请求 @return 已保留的待激活用户 */
    public TenantUserVO create(CreateTenantUserRequest request) {
        TenantActorContext actor = actorService.current();
        authClient.consumeTenantReauthTicket(actor.accountId(), request.reauthTicket(), "TENANT_USER_CREATE");
        TenantUserCommandService.UserProvisionContext context = commandService.create(actor, request);
        try {
            authClient.provision(context.accountId(), context.tenantId(), context.organizationId());
            commandService.markProvisionPublished(context);
        } catch (RuntimeException exception) {
            commandService.markProvisionRetry(context, "AUTH_PROVISION_UNAVAILABLE");
            throw new BusinessException("IAM_2805", "用户已保留，认证凭证等待服务恢复后重试", 502);
        }
        return detail(context.accountId());
    }

    /** @param accountId 目标账号 @param request 角色替换请求 @return 更新后的用户 */
    public TenantUserVO replaceRoles(long accountId, ReplaceTenantUserRolesRequest request) {
        TenantActorContext actor = actorService.current();
        authClient.consumeTenantReauthTicket(actor.accountId(), request.reauthTicket(),
                "TENANT_USER_ROLE_ASSIGN");
        commandService.replaceRoles(actor, accountId, request);
        return detail(accountId);
    }

    /** @param accountId 目标账号 @param request 资料修改请求 @return 更新后用户 */
    public TenantUserVO update(long accountId, UpdateTenantUserRequest request) {
        TenantActorContext actor = actorService.current();
        commandService.update(actor, accountId, request);
        return detail(accountId);
    }

    /** @param accountId 目标账号 @param request 状态命令 @param enable 是否恢复 @return 更新后用户 */
    public TenantUserVO changeStatus(long accountId, TenantUserStatusRequest request, boolean enable) {
        TenantActorContext actor = actorService.current();
        commandService.changeStatus(actor, accountId, request, enable);
        return detail(accountId);
    }

    /** @param accountId 目标账号 @param request 注销命令 */
    public void cancel(long accountId, TenantUserStatusRequest request) {
        TenantActorContext actor = actorService.current();
        commandService.cancel(actor, accountId, request);
    }

    /** @param accountId 待激活账号 @return 新的一次性激活票据 */
    public TenantUserActivationLinkVO issueActivationLink(long accountId) {
        TenantActorContext actor = actorService.current();
        TenantUserMapper.UserRow user = requireUser(actor, accountId);
        if (!"PENDING_ACTIVATION".equals(user.status())) {
            throw new BusinessException("IAM_2302", "账号不处于待激活状态", 409);
        }
        authClient.provision(accountId, actor.tenantId(), actor.organizationId());
        TenantActivationTicketInternalResponse result = authClient.issueActivationTicket(accountId);
        return new TenantUserActivationLinkVO(result.activationTicket(), result.expiresAt());
    }

    /**
     * 为授权范围内本机构用户生成三十分钟一次性密码重置票据。
     *
     * @param accountId 目标账号 ID
     * @param request 重置请求
     * @return 一次性重置票据
     */
    @AuditOperation(
            source = "IAM", categoryPath = {"租户端", "用户管理", "密码重置"},
            actionCode = "iam:user:password-reset", actionName = "管理员发起密码重置",
            targetType = "TENANT_ACCOUNT", targetId = "#accountId", reason = "#request.reason"
    )
    public TenantUserPasswordResetLinkVO issuePasswordResetLink(
            long accountId,
            TenantUserPasswordResetRequest request
    ) {
        TenantActorContext actor = actorService.current();
        TenantUserMapper.UserRow user = requireUser(actor, accountId);
        if (!("ACTIVE".equals(user.status()) || "PASSWORD_RESET_REQUIRED".equals(user.status()))) {
            throw new BusinessException("IAM_2811", "目标账号当前不能发起密码重置", 409);
        }
        if (user.owner() && !actor.organizationOwner()) {
            throw new BusinessException("IAM_2812", "普通管理员不能重置机构所有者密码", 403);
        }
        if (!actor.organizationOwner() && mapper.countManagementPermissions(
                actor.tenantId(), actor.organizationId(), accountId) > 0) {
            throw new BusinessException("IAM_2813", "普通管理员之间默认不能互相重置密码", 403);
        }
        TenantPasswordResetTicketInternalResponse result = authClient.issuePasswordResetTicket(
                accountId, "TENANT_ACCOUNT", actor.accountId(), request.reauthTicket(),
                "TENANT_USER_PASSWORD_RESET"
        );
        passwordResetService.requirePasswordReset(accountId, actor.accountId());
        return new TenantUserPasswordResetLinkVO(result.resetTicket(), result.expiresAt());
    }

    /** @return 当前机构用户，不存在则抛错 */
    private TenantUserMapper.UserRow requireUser(TenantActorContext actor, long accountId) {
        TenantUserMapper.UserRow row = mapper.selectUser(actor.tenantId(), actor.organizationId(), accountId);
        if (row == null) throw new BusinessException("IAM_2804", "用户不存在", 404);
        return row;
    }

    /** 批量组装角色，避免用户分页产生 N+1。 */
    private List<TenantUserVO> toVOs(TenantActorContext actor, List<TenantUserMapper.UserRow> rows) {
        if (rows.isEmpty()) return List.of();
        List<Long> accountIds = rows.stream().map(TenantUserMapper.UserRow::id).toList();
        Map<Long, List<TenantUserRoleVO>> roles = new LinkedHashMap<>();
        for (TenantUserMapper.UserRoleRow role : mapper.selectUserRoles(
                actor.tenantId(), actor.organizationId(), accountIds)) {
            roles.computeIfAbsent(role.accountId(), ignored -> new ArrayList<>())
                    .add(new TenantUserRoleVO(Long.toString(role.roleId()), role.roleName()));
        }
        return rows.stream().map(row -> new TenantUserVO(
                Long.toString(row.id()), row.username(), row.displayName(), row.employeeNumber(),
                Long.toString(row.organizationId()), row.organizationName(), row.status(), row.validityMode(),
                row.effectiveAt(), row.expiresAt(), List.copyOf(roles.getOrDefault(row.id(), List.of())),
                row.owner(), row.authzVersion(), row.createdTime(), row.version()
        )).toList();
    }
}
