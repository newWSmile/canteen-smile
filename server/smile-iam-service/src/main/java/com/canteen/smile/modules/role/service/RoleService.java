package com.canteen.smile.modules.role.service;

import com.canteen.smile.audit.spi.AuditClientIpResolver;
import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.audit.service.IamAuditLogService;
import com.canteen.smile.modules.role.dto.CreateRoleRequest;
import com.canteen.smile.modules.role.dto.ReplaceRoleDataPolicyRequest;
import com.canteen.smile.modules.role.dto.ReplaceRolePermissionsRequest;
import com.canteen.smile.modules.role.dto.RoleDataPolicyItemRequest;
import com.canteen.smile.modules.role.dto.RolePageQuery;
import com.canteen.smile.modules.role.dto.RoleStatusRequest;
import com.canteen.smile.modules.role.dto.UpdateRoleRequest;
import com.canteen.smile.modules.role.entity.RoleEntity;
import com.canteen.smile.modules.role.mapper.RoleMapper;
import com.canteen.smile.modules.role.vo.GrantBoundaryVO;
import com.canteen.smile.modules.role.vo.RoleDataPolicyVO;
import com.canteen.smile.modules.role.vo.RolePermissionVO;
import com.canteen.smile.modules.role.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 本机构角色、功能权限和数据范围管理服务。 */
@Service
@RequiredArgsConstructor
public class RoleService {

    /** 角色数据访问接口。 */
    private final RoleMapper mapper;
    /** 当前租户操作人服务。 */
    private final TenantActorService actorService;
    /** IAM 审计服务。 */
    private final IamAuditLogService auditLogService;
    /** 当前受信任客户端 IP 解析器。 */
    private final AuditClientIpResolver clientIpResolver;

    /** @param query 分页条件 @return 当前机构角色分页 */
    @Transactional(readOnly = true)
    public PageResult<RoleVO> page(RolePageQuery query) {
        TenantActorContext actor = actorService.current();
        long total = mapper.countRoles(actor.tenantId(), actor.organizationId(), query.getStatus());
        List<RoleVO> items = mapper.selectRoles(
                actor.tenantId(), actor.organizationId(), query.getStatus(),
                (query.getPageNo() - 1) * query.getPageSize(), query.getPageSize()
        ).stream().map(this::toVO).toList();
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), total);
    }

    /** @param roleId 角色 ID @return 当前机构角色详情 */
    @Transactional(readOnly = true)
    public RoleVO detail(long roleId) {
        TenantActorContext actor = actorService.current();
        return toVO(requireRole(actor, roleId));
    }

    /** @param request 新建参数 @return 新建角色 */
    @Transactional
    public RoleVO create(CreateRoleRequest request) {
        TenantActorContext actor = actorService.current();
        List<Long> specifiedOrganizationIds = distinctIds(request.specifiedOrganizationIds());
        validateScope(actor, request.defaultScopeType(), specifiedOrganizationIds);
        long roleId = mapper.nextRoleId();
        RoleEntity entity = new RoleEntity();
        entity.setId(roleId);
        entity.setTenantId(actor.tenantId());
        entity.setOrganizationId(actor.organizationId());
        entity.setRoleCode("ROLE_T" + actor.tenantId() + "_O" + actor.organizationId() + "_R" + roleId);
        entity.setName(request.name().strip());
        entity.setNormalizedName(normalizeName(request.name()));
        entity.setDescription(trimToNull(request.description()));
        entity.setRoleType("CUSTOM");
        entity.setStatus("ACTIVE");
        entity.setCreatedBy(actor.accountId());
        entity.setUpdatedBy(actor.accountId());
        try {
            mapper.insertRole(entity);
            insertPolicy(actor, roleId, "*", request.defaultScopeType(), specifiedOrganizationIds);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("IAM_2701", "本机构角色名称已经存在", 409);
        }
        audit(actor, "iam:role:create", "新增角色", roleId, null);
        return toVO(requireRole(actor, roleId));
    }

    /** @param roleId 角色 ID @param request 修改参数 @return 修改后的角色 */
    @Transactional
    public RoleVO update(long roleId, UpdateRoleRequest request) {
        TenantActorContext actor = actorService.current();
        requireMutableRole(actor, roleId);
        try {
            if (mapper.updateRole(
                    actor.tenantId(), actor.organizationId(), roleId, request.name().strip(),
                    normalizeName(request.name()), trimToNull(request.description()), request.version(),
                    actor.accountId()) != 1) {
                throw conflict();
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("IAM_2701", "本机构角色名称已经存在", 409);
        }
        audit(actor, "iam:role:update", "修改角色", roleId, null);
        return toVO(requireRole(actor, roleId));
    }

    /** @param roleId 角色 ID @param enabled 是否启用 @param request 状态命令 @return 修改后的角色 */
    @Transactional
    public RoleVO changeStatus(long roleId, boolean enabled, RoleStatusRequest request) {
        TenantActorContext actor = actorService.current();
        RoleMapper.RoleRow current = requireMutableRole(actor, roleId);
        String targetStatus = enabled ? "ACTIVE" : "DISABLED";
        if (targetStatus.equals(current.status())) return toVO(current);
        if (mapper.updateRoleStatus(actor.tenantId(), actor.organizationId(), roleId, targetStatus,
                request.version(), actor.accountId()) != 1) {
            throw conflict();
        }
        authorizationChanged(actor, roleId);
        audit(actor, enabled ? "iam:role:enable" : "iam:role:disable",
                enabled ? "启用角色" : "停用角色", roleId, request.reason().strip());
        return toVO(requireRole(actor, roleId));
    }

    /** @param roleId 角色 ID @param request 删除命令 */
    @Transactional
    public void delete(long roleId, RoleStatusRequest request) {
        TenantActorContext actor = actorService.current();
        requireMutableRole(actor, roleId);
        if (mapper.countRoleAccounts(actor.tenantId(), actor.organizationId(), roleId) > 0) {
            throw new BusinessException("IAM_2702", "角色仍有关联账号，请先解除账号角色", 409);
        }
        if (mapper.deleteRole(actor.tenantId(), actor.organizationId(), roleId,
                request.version(), actor.accountId()) != 1) {
            throw conflict();
        }
        audit(actor, "iam:role:delete", "删除角色", roleId, request.reason().strip());
    }

    /** @return 当前操作者授权上限 */
    @Transactional(readOnly = true)
    public GrantBoundaryVO grantBoundary() {
        TenantActorContext actor = actorService.current();
        List<String> permissionIds = mapper.selectGrantablePermissions(
                actor.tenantId(), actor.accountId(), actor.rootOwner()
        ).stream().map(row -> Long.toString(row.id())).toList();
        return new GrantBoundaryVO(
                Long.toString(actor.organizationId()), actor.rootOwner(), permissionIds,
                actor.rootOwner()
                        ? List.of("SELF", "CURRENT_ORG", "CURRENT_ORG_AND_DESCENDANTS",
                                "SPECIFIED_ORGS", "SPECIFIED_ORGS_AND_DESCENDANTS", "TENANT_ALL")
                        : List.of("SELF", "CURRENT_ORG")
        );
    }

    /** @param roleId 可选角色 ID @return 当前可分配权限树及选中状态 */
    @Transactional(readOnly = true)
    public List<RolePermissionVO> permissionTree(Long roleId) {
        TenantActorContext actor = actorService.current();
        Set<Long> grantedIds = roleId == null ? Set.of() : new LinkedHashSet<>(
                mapper.selectRolePermissionIds(actor.tenantId(), actor.organizationId(),
                        requireRole(actor, roleId).id())
        );
        return mapper.selectGrantablePermissions(actor.tenantId(), actor.accountId(), actor.rootOwner())
                .stream().map(row -> new RolePermissionVO(
                        Long.toString(row.id()), row.parentId() == null ? null : Long.toString(row.parentId()),
                        row.permissionCode(), row.name(), row.resourceType(), row.appCode(), row.featureCode(),
                        row.sortOrder(), grantedIds.contains(row.id())
                )).toList();
    }

    /** @param roleId 角色 ID @param request 完整权限集合 @return 更新后的权限树 */
    @Transactional
    public List<RolePermissionVO> replacePermissions(long roleId, ReplaceRolePermissionsRequest request) {
        TenantActorContext actor = actorService.current();
        requireMutableRole(actor, roleId);
        List<Long> permissionIds = distinctIds(request.permissionIds());
        if (!permissionIds.isEmpty() && mapper.countGrantablePermissionIds(
                actor.tenantId(), actor.accountId(), actor.rootOwner(), permissionIds) != permissionIds.size()) {
            throw new BusinessException("IAM_2007", "分配权限超出当前操作者授权上限", 403);
        }
        if (mapper.touchRoleAuthorization(actor.tenantId(), actor.organizationId(), roleId,
                request.version(), actor.accountId()) != 1) {
            throw conflict();
        }
        mapper.deactivateRolePermissions(actor.tenantId(), actor.organizationId(), roleId, actor.accountId());
        if (!permissionIds.isEmpty()) {
            mapper.upsertRolePermissions(actor.tenantId(), actor.organizationId(), roleId,
                    permissionIds, actor.accountId());
        }
        authorizationChanged(actor, roleId);
        audit(actor, "iam:role:grant", "分配功能权限", roleId, request.reason().strip());
        return permissionTree(roleId);
    }

    /** @param roleId 角色 ID @return 当前数据策略 */
    @Transactional(readOnly = true)
    public List<RoleDataPolicyVO> dataPolicies(long roleId) {
        TenantActorContext actor = actorService.current();
        requireRole(actor, roleId);
        Map<Long, MutablePolicy> policies = new LinkedHashMap<>();
        for (RoleMapper.DataPolicyRow row
                : mapper.selectRoleDataPolicies(actor.tenantId(), actor.organizationId(), roleId)) {
            MutablePolicy policy = policies.computeIfAbsent(row.policyId(), ignored ->
                    new MutablePolicy(row.moduleCode(), row.moduleName(), row.scopeType(), new ArrayList<>()));
            if (row.organizationId() != null) policy.organizationIds().add(Long.toString(row.organizationId()));
        }
        return policies.values().stream().map(policy -> new RoleDataPolicyVO(
                policy.moduleCode(), policy.moduleName(), policy.scopeType(), policy.organizationIds()
        )).toList();
    }

    /** @param roleId 角色 ID @param request 完整数据策略 @return 保存后的策略 */
    @Transactional
    public List<RoleDataPolicyVO> replaceDataPolicies(long roleId, ReplaceRoleDataPolicyRequest request) {
        TenantActorContext actor = actorService.current();
        requireMutableRole(actor, roleId);
        validatePolicies(actor, request.policies());
        if (mapper.touchRoleAuthorization(actor.tenantId(), actor.organizationId(), roleId,
                request.version(), actor.accountId()) != 1) {
            throw conflict();
        }
        mapper.deactivateRoleDataScopeOrganizations(actor.tenantId(), actor.organizationId(), roleId,
                actor.accountId());
        mapper.deactivateRoleDataPolicies(actor.tenantId(), actor.organizationId(), roleId, actor.accountId());
        for (RoleDataPolicyItemRequest policy : request.policies()) {
            insertPolicy(actor, roleId, policy.moduleCode(), policy.scopeType(), distinctIds(policy.organizationIds()));
        }
        authorizationChanged(actor, roleId);
        audit(actor, "iam:role:data-scope", "配置数据范围", roleId, request.reason().strip());
        return dataPolicies(roleId);
    }

    /** 校验默认范围唯一、模块真实发布且指定机构均位于当前机构子树。 */
    private void validatePolicies(TenantActorContext actor, List<RoleDataPolicyItemRequest> policies) {
        Set<String> moduleCodes = new LinkedHashSet<>();
        for (RoleDataPolicyItemRequest policy : policies) {
            String moduleCode = policy.moduleCode().strip();
            if (!moduleCodes.add(moduleCode)) {
                throw new BusinessException("IAM_2704", "数据范围模块不能重复", 400);
            }
            validateScope(actor, policy.scopeType(), distinctIds(policy.organizationIds()));
        }
        if (!moduleCodes.contains("*")) {
            throw new BusinessException("IAM_2704", "角色必须包含一条默认数据范围", 400);
        }
        List<String> publishedModuleCodes = moduleCodes.stream().filter(code -> !"*".equals(code)).toList();
        if (!publishedModuleCodes.isEmpty()
                && mapper.countPublishedDataModules(publishedModuleCodes) != publishedModuleCodes.size()) {
            throw new BusinessException("IAM_2704", "模块覆盖只能引用已发布的数据模块", 400);
        }
    }

    /** 校验操作者范围以及指定机构参数。 */
    private void validateScope(TenantActorContext actor, String scopeType, List<Long> organizationIds) {
        boolean specified = "SPECIFIED_ORGS".equals(scopeType)
                || "SPECIFIED_ORGS_AND_DESCENDANTS".equals(scopeType);
        if (specified != !organizationIds.isEmpty()) {
            throw new BusinessException("IAM_2704", specified ? "指定机构范围不能为空" : "当前范围不能携带指定机构", 400);
        }
        if (!actor.rootOwner() && !("SELF".equals(scopeType) || "CURRENT_ORG".equals(scopeType))) {
            throw new BusinessException("IAM_2008", "分配数据范围超出当前操作者授权上限", 403);
        }
        if (!organizationIds.isEmpty() && mapper.countOrganizationsInActorSubtree(
                actor.tenantId(), actor.organizationId(), organizationIds) != organizationIds.size()) {
            throw new BusinessException("IAM_2008", "指定机构超出当前操作者机构子树", 403);
        }
    }

    /** 插入单条策略及其指定机构。 */
    private void insertPolicy(TenantActorContext actor, long roleId, String moduleCode,
                              String scopeType, List<Long> organizationIds) {
        long policyId = mapper.nextDataPolicyId();
        mapper.insertDataPolicy(policyId, actor.tenantId(), actor.organizationId(), roleId,
                moduleCode.strip(), scopeType, actor.accountId());
        if (!organizationIds.isEmpty()) {
            mapper.insertDataPolicyOrganizations(actor.tenantId(), policyId, organizationIds, actor.accountId());
        }
    }

    /** 批量提升账号授权版本并写入可重试事件。 */
    private void authorizationChanged(TenantActorContext actor, long roleId) {
        mapper.bumpAssignedAccountAuthzVersions(actor.tenantId(), actor.organizationId(), roleId, actor.accountId());
        mapper.insertRoleAuthorizationOutbox(
                UUID.randomUUID().toString(), actor.tenantId(), actor.organizationId(), roleId, actor.accountId(),
                clientIpResolver.resolve()
        );
    }

    /** @return 当前机构角色，不存在时抛错 */
    private RoleMapper.RoleRow requireRole(TenantActorContext actor, long roleId) {
        RoleMapper.RoleRow row = mapper.selectRole(actor.tenantId(), actor.organizationId(), roleId);
        if (row == null) throw new BusinessException("IAM_2705", "角色不存在", 404);
        return row;
    }

    /** @return 可修改的自定义角色 */
    private RoleMapper.RoleRow requireMutableRole(TenantActorContext actor, long roleId) {
        RoleMapper.RoleRow row = requireRole(actor, roleId);
        if (!"CUSTOM".equals(row.roleType())) {
            throw new BusinessException("IAM_2009", "机构所有者角色受系统保护，不能修改", 409);
        }
        return row;
    }

    /** @return 去重后的数字 ID */
    private List<Long> distinctIds(List<String> values) {
        return values.stream().map(Long::parseLong).distinct().toList();
    }

    /** @return 小写且去空白的名称唯一键 */
    private String normalizeName(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    /** @return 去空白后的可空文本 */
    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * @param actor 当前租户操作者
     * @param action 稳定动作编码
     * @param actionName 中文动作名称
     * @param roleId 目标角色 ID
     * @param reason 敏感操作原因
     */
    private void audit(TenantActorContext actor, String action, String actionName, long roleId, String reason) {
        auditLogService.recordTenantOrganizationAction(
                actor.tenantId(), actor.organizationId(), actor.accountId(), action, actionName,
                "ROLE", Long.toString(roleId), reason, "SUCCESS"
        );
    }

    /** @return 角色响应 */
    private RoleVO toVO(RoleMapper.RoleRow row) {
        return new RoleVO(Long.toString(row.id()), row.roleCode(), row.name(), row.description(), row.roleType(),
                row.status(), row.authzVersion(), row.accountCount(), row.defaultScopeType(),
                row.createdTime(), row.version());
    }

    /** @return 乐观锁冲突 */
    private BusinessException conflict() {
        return new BusinessException("IAM_2006", "角色状态已变化，请刷新后重试", 409);
    }

    /** 服务内部聚合同一策略的指定机构。 */
    private record MutablePolicy(String moduleCode, String moduleName, String scopeType,
                                 List<String> organizationIds) {
    }
}
