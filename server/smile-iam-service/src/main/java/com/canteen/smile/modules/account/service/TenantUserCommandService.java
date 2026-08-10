package com.canteen.smile.modules.account.service;

import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.dto.CreateTenantUserRequest;
import com.canteen.smile.modules.account.dto.ReplaceTenantUserRolesRequest;
import com.canteen.smile.modules.account.dto.TenantUserStatusRequest;
import com.canteen.smile.modules.account.dto.UpdateTenantUserRequest;
import com.canteen.smile.modules.account.mapper.TenantUserMapper;
import com.canteen.smile.modules.audit.service.IamAuditLogService;
import com.canteen.smile.modules.platform.service.UsernameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** 租户用户写操作的 IAM 本地事务边界。 */
@Service
@RequiredArgsConstructor
public class TenantUserCommandService {

    /** 用户数据访问接口。 */
    private final TenantUserMapper mapper;
    /** IAM 审计服务。 */
    private final IamAuditLogService auditLogService;

    /** @param actor 当前操作者 @param request 创建请求 @return 跨 Auth 编排上下文 */
    @Transactional
    public UserProvisionContext create(TenantActorContext actor, CreateTenantUserRequest request) {
        long organizationId = Long.parseLong(request.organizationId());
        if (organizationId != actor.organizationId()) {
            throw new BusinessException("IAM_2801", "只能在当前机构创建用户", 403);
        }
        List<Long> roleIds = distinctIds(request.roleIds());
        validateAssignableRoles(actor, roleIds);
        Validity validity = validity(request.validityMode(), request.effectiveAt(), request.expiresAt());
        String username = request.username().strip();
        String normalizedUsername = UsernameNormalizer.normalize(username);
        String employeeNumber = trimToNull(request.employeeNumber());
        String normalizedEmployeeNumber = employeeNumber == null ? null : normalizeEmployeeNumber(employeeNumber);
        if (mapper.countReservedUsername(normalizedUsername) > 0) {
            throw new BusinessException("IAM_2001", "用户名已经被当前或历史账号占用", 409);
        }
        if (normalizedEmployeeNumber != null && mapper.countReservedEmployeeNumber(
                actor.tenantId(), organizationId, normalizedEmployeeNumber) > 0) {
            throw new BusinessException("IAM_2802", "工号已经在本机构永久占用", 409);
        }
        long accountId = mapper.nextAccountId();
        long outboxId = mapper.nextOutboxId();
        try {
            mapper.insertAccount(accountId, actor.tenantId(), organizationId, username, normalizedUsername,
                    trimToNull(request.displayName()), employeeNumber, validity.mode(), validity.effectiveAt(),
                    validity.expiresAt(), actor.accountId());
            mapper.insertUsernameRegistry(accountId, username, normalizedUsername, actor.accountId());
            if (employeeNumber != null) {
                mapper.insertEmployeeNumberRegistry(accountId, actor.tenantId(), organizationId, employeeNumber,
                        normalizedEmployeeNumber, actor.accountId());
            }
            mapper.insertAccountRoles(actor.tenantId(), organizationId, accountId, roleIds, actor.accountId());
            mapper.insertProvisionOutbox(outboxId, UUID.randomUUID().toString(), actor.tenantId(), organizationId,
                    accountId, actor.accountId());
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("IAM_2803", "用户名或工号已被占用，请刷新后重试", 409);
        }
        auditLogService.recordTenantOrganizationAction(actor.tenantId(), actor.organizationId(), actor.accountId(),
                "iam:user:create",
                "TENANT_ACCOUNT", Long.toString(accountId), request.reason().strip(), "SUCCESS");
        return new UserProvisionContext(accountId, actor.tenantId(), organizationId, outboxId, actor.accountId());
    }

    /** @param context 创建编排上下文 */
    @Transactional
    public void markProvisionPublished(UserProvisionContext context) {
        mapper.markProvisionPublished(context.outboxId(), context.operatorId());
    }

    /** @param context 创建编排上下文 @param errorCode 非敏感错误码 */
    @Transactional
    public void markProvisionRetry(UserProvisionContext context, String errorCode) {
        mapper.markProvisionRetry(context.outboxId(), context.operatorId(), errorCode);
    }

    /** @param actor 当前操作者 @param accountId 目标账号 @param request 角色替换请求 */
    @Transactional
    public void replaceRoles(TenantActorContext actor, long accountId, ReplaceTenantUserRolesRequest request) {
        TenantUserMapper.UserRow account = requireMutableUser(actor, accountId);
        List<Long> roleIds = distinctIds(request.roleIds());
        validateAssignableRoles(actor, roleIds);
        if (mapper.bumpAuthzVersion(actor.tenantId(), actor.organizationId(), accountId, request.version(),
                actor.accountId()) != 1) {
            throw new BusinessException("IAM_2006", "用户状态已变化，请刷新后重试", 409);
        }
        mapper.deactivateAccountRoles(actor.tenantId(), actor.organizationId(), accountId, actor.accountId());
        mapper.insertAccountRoles(actor.tenantId(), actor.organizationId(), accountId, roleIds, actor.accountId());
        mapper.insertRolesChangedOutbox(mapper.nextOutboxId(), UUID.randomUUID().toString(), actor.tenantId(),
                accountId, actor.accountId());
        auditLogService.recordTenantOrganizationAction(actor.tenantId(), actor.organizationId(), actor.accountId(),
                "iam:user:role-assign", "TENANT_ACCOUNT", Long.toString(accountId),
                request.reason().strip(), "SUCCESS");
    }

    /** @param actor 当前操作者 @param accountId 目标账号 @param request 资料修改请求 */
    @Transactional
    public void update(TenantActorContext actor, long accountId, UpdateTenantUserRequest request) {
        TenantUserMapper.UserRow account = requireMutableUser(actor, accountId);
        Validity validity = validity(request.validityMode(), request.effectiveAt(), request.expiresAt());
        String employeeNumber = trimToNull(request.employeeNumber());
        String normalizedEmployeeNumber = employeeNumber == null ? null : normalizeEmployeeNumber(employeeNumber);
        String currentNormalizedEmployeeNumber = account.employeeNumber() == null
                ? null : normalizeEmployeeNumber(account.employeeNumber());
        if (!Objects.equals(normalizedEmployeeNumber, currentNormalizedEmployeeNumber)
                && normalizedEmployeeNumber != null
                && mapper.countReservedEmployeeNumber(actor.tenantId(), actor.organizationId(),
                normalizedEmployeeNumber) > 0) {
            throw new BusinessException("IAM_2802", "工号已经在本机构永久占用", 409);
        }
        boolean validityChanged = !Objects.equals(validity.mode(), account.validityMode())
                || !Objects.equals(validity.effectiveAt(), account.effectiveAt())
                || !Objects.equals(validity.expiresAt(), account.expiresAt());
        try {
            if (!Objects.equals(normalizedEmployeeNumber, currentNormalizedEmployeeNumber)
                    && employeeNumber != null) {
                mapper.insertEmployeeNumberRegistry(accountId, actor.tenantId(), actor.organizationId(),
                        employeeNumber, normalizedEmployeeNumber, actor.accountId());
            }
            if (mapper.updateUserProfile(actor.tenantId(), actor.organizationId(), accountId,
                    trimToNull(request.displayName()), employeeNumber, validity.mode(), validity.effectiveAt(),
                    validity.expiresAt(), validityChanged, request.version(), actor.accountId()) != 1) {
                throw concurrentChange();
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("IAM_2802", "工号已经在本机构永久占用", 409);
        }
        if (validityChanged) {
            insertAccountChangedEvent(actor, accountId, "ACCOUNT_VALIDITY_CHANGED");
        }
        auditLogService.recordTenantOrganizationAction(actor.tenantId(), actor.organizationId(), actor.accountId(),
                "iam:user:update",
                "TENANT_ACCOUNT", Long.toString(accountId), request.reason().strip(), "SUCCESS");
    }

    /** @param actor 当前操作者 @param accountId 目标账号 @param request 命令参数 @param enable 是否恢复 */
    @Transactional
    public void changeStatus(TenantActorContext actor, long accountId, TenantUserStatusRequest request,
                             boolean enable) {
        TenantUserMapper.UserRow account = requireMutableUser(actor, accountId);
        String currentStatus = enable ? "DISABLED" : "ACTIVE";
        String targetStatus = enable ? "ACTIVE" : "DISABLED";
        if (!currentStatus.equals(account.status())) {
            throw new BusinessException("IAM_2808", enable ? "只有已停用账号可以恢复" : "只有正常账号可以停用", 409);
        }
        if (mapper.changeUserStatus(actor.tenantId(), actor.organizationId(), accountId, currentStatus,
                targetStatus, request.version(), actor.accountId()) != 1) {
            throw concurrentChange();
        }
        insertAccountChangedEvent(actor, accountId, enable ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED");
        auditLogService.recordTenantOrganizationAction(actor.tenantId(), actor.organizationId(), actor.accountId(),
                enable ? "iam:user:enable" : "iam:user:disable", "TENANT_ACCOUNT", Long.toString(accountId),
                request.reason().strip(), "SUCCESS");
    }

    /** @param actor 当前操作者 @param accountId 目标账号 @param request 不可恢复注销命令 */
    @Transactional
    public void cancel(TenantActorContext actor, long accountId, TenantUserStatusRequest request) {
        TenantUserMapper.UserRow account = requireMutableUser(actor, accountId);
        if (mapper.changeUserStatus(actor.tenantId(), actor.organizationId(), accountId, account.status(),
                "CANCELLED", request.version(), actor.accountId()) != 1) {
            throw concurrentChange();
        }
        mapper.disableUsernameLogin(accountId, actor.accountId());
        mapper.deactivateAccountRoles(actor.tenantId(), actor.organizationId(), accountId, actor.accountId());
        insertAccountChangedEvent(actor, accountId, "ACCOUNT_CANCELLED");
        auditLogService.recordTenantOrganizationAction(actor.tenantId(), actor.organizationId(), actor.accountId(),
                "iam:user:cancel",
                "TENANT_ACCOUNT", Long.toString(accountId), request.reason().strip(), "SUCCESS");
    }

    /** 校验目标角色均为本机构有效自定义角色且不超过操作者授权上限。 */
    private void validateAssignableRoles(TenantActorContext actor, List<Long> roleIds) {
        if (roleIds.isEmpty() || mapper.countAssignableRoles(actor.tenantId(), actor.organizationId(),
                actor.accountId(), actor.rootOwner(), roleIds) != roleIds.size()) {
            throw new BusinessException("IAM_2012", "账号至少需要一个当前管理员可授予的有效角色", 400);
        }
    }

    /** @return 可修改的同机构非所有者账号 */
    private TenantUserMapper.UserRow requireMutableUser(TenantActorContext actor, long accountId) {
        TenantUserMapper.UserRow account = mapper.selectUser(actor.tenantId(), actor.organizationId(), accountId);
        if (account == null) throw new BusinessException("IAM_2804", "用户不存在", 404);
        if (account.owner()) throw new BusinessException("IAM_2009", "机构所有者不能通过普通用户管理修改", 409);
        return account;
    }

    /** 写入等待 Auth 幂等消费的账号安全变化事件。 */
    private void insertAccountChangedEvent(TenantActorContext actor, long accountId, String eventType) {
        mapper.insertAccountChangedOutbox(mapper.nextOutboxId(), UUID.randomUUID().toString(), actor.tenantId(),
                accountId, eventType, actor.accountId());
    }

    /** @return 统一乐观锁冲突异常 */
    private BusinessException concurrentChange() {
        return new BusinessException("IAM_2006", "用户状态已变化，请刷新后重试", 409);
    }

    /** @return 去重数字 ID */
    private List<Long> distinctIds(List<String> values) {
        return values.stream().map(Long::parseLong).distinct().toList();
    }

    /** @return 已校验有效期 */
    private Validity validity(String mode, OffsetDateTime effectiveAt, OffsetDateTime expiresAt) {
        if ("LONG_TERM".equals(mode)) {
            if (effectiveAt != null || expiresAt != null) {
                throw new BusinessException("IAM_2807", "长期有效账号不能设置生效或到期时间", 400);
            }
            return new Validity(mode, null, null);
        }
        if (effectiveAt == null || expiresAt == null || !effectiveAt.isBefore(expiresAt)) {
            throw new BusinessException("IAM_2807", "固定有效期必须提供合法的生效和到期时间", 400);
        }
        return new Validity(mode, effectiveAt, expiresAt);
    }

    /** @return 可空去空白文本 */
    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** @return 工号的 NFKC 小写唯一键 */
    private String normalizeEmployeeNumber(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    /** 账号初始化跨服务编排上下文。 */
    public record UserProvisionContext(long accountId, long tenantId, long organizationId,
                                       long outboxId, long operatorId) { }
    /** 账号有效期值对象。 */
    private record Validity(String mode, OffsetDateTime effectiveAt, OffsetDateTime expiresAt) { }
}
