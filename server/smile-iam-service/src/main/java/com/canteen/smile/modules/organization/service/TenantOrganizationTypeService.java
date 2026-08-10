package com.canteen.smile.modules.organization.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.audit.service.IamAuditLogService;
import com.canteen.smile.modules.organization.dto.ChangeOrganizationTypeStatusRequest;
import com.canteen.smile.modules.organization.dto.CreateOrganizationTypeRequest;
import com.canteen.smile.modules.organization.dto.OrganizationTypePageQuery;
import com.canteen.smile.modules.organization.dto.OrganizationTypeRelationRequest;
import com.canteen.smile.modules.organization.dto.ReplaceOrganizationTypeRelationsRequest;
import com.canteen.smile.modules.organization.dto.UpdateOrganizationTypeRequest;
import com.canteen.smile.modules.organization.entity.OrganizationTypeEntity;
import com.canteen.smile.modules.organization.mapper.TenantOrganizationMapper;
import com.canteen.smile.modules.organization.vo.OrganizationTypeRelationVO;
import com.canteen.smile.modules.organization.vo.OrganizationTypeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/** 租户独立机构类型和允许关系管理服务。 */
@Service
@RequiredArgsConstructor
public class TenantOrganizationTypeService {

    /** 机构类型不存在错误码。 */
    private static final String TYPE_NOT_FOUND_CODE = "IAM_2501";

    /** 机构类型关系非法错误码。 */
    private static final String INVALID_RELATION_CODE = "IAM_2502";

    /** 机构类型并发冲突错误码。 */
    private static final String TYPE_CONFLICT_CODE = "IAM_2503";

    /** 当前租户操作人服务。 */
    private final TenantActorService actorService;

    /** 机构治理数据访问接口。 */
    private final TenantOrganizationMapper mapper;

    /** IAM 管理审计服务。 */
    private final IamAuditLogService auditLogService;

    /** @param query 分页条件 @return 当前租户机构类型分页 */
    @Transactional(readOnly = true)
    public PageResult<OrganizationTypeVO> page(OrganizationTypePageQuery query) {
        TenantActorContext actor = actorService.current();
        long total = mapper.countOrganizationTypes(actor.tenantId(), query.getStatus());
        List<OrganizationTypeVO> items = mapper.selectOrganizationTypes(
                actor.tenantId(),
                query.getStatus(),
                (query.getPageNo() - 1) * query.getPageSize(),
                query.getPageSize()
        ).stream().map(this::toVO).toList();
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), total);
    }

    /** @return 当前租户全部有效类型，供表单选择 */
    @Transactional(readOnly = true)
    public List<OrganizationTypeVO> activeTypes() {
        TenantActorContext actor = actorService.current();
        return mapper.selectActiveOrganizationTypes(actor.tenantId()).stream().map(this::toVO).toList();
    }

    /** @return 当前租户全部有效允许关系 */
    @Transactional(readOnly = true)
    public List<OrganizationTypeRelationVO> relations() {
        TenantActorContext actor = actorService.current();
        return mapper.selectOrganizationTypeRelations(actor.tenantId()).stream()
                .map(this::toRelationVO)
                .toList();
    }

    /** @param request 新类型参数 @return 新增类型 */
    @Transactional
    public OrganizationTypeVO create(CreateOrganizationTypeRequest request) {
        TenantActorContext actor = actorService.requireRootOwner();
        OrganizationTypeEntity entity = new OrganizationTypeEntity();
        entity.setTenantId(actor.tenantId());
        entity.setTypeCode(request.typeCode().strip().toUpperCase(Locale.ROOT));
        entity.setName(request.name().strip());
        entity.setSortOrder(request.sortOrder());
        entity.setStatus("ACTIVE");
        entity.setCreatedBy(actor.accountId());
        entity.setUpdatedBy(actor.accountId());
        try {
            mapper.insertOrganizationType(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(TYPE_CONFLICT_CODE, "机构类型编码已经被永久占用", 409);
        }
        audit(actor, "iam:org-type:create", "ORG_TYPE", entity.getId().toString(), null);
        return toVO(requireType(actor.tenantId(), entity.getId()));
    }

    /** @param typeId 类型 ID @param request 修改参数 @return 修改后的类型 */
    @Transactional
    public OrganizationTypeVO update(long typeId, UpdateOrganizationTypeRequest request) {
        TenantActorContext actor = actorService.requireRootOwner();
        requireType(actor.tenantId(), typeId);
        if (mapper.updateOrganizationType(
                actor.tenantId(), typeId, request.name().strip(), request.sortOrder(),
                request.version(), actor.accountId()) != 1) {
            throw conflict();
        }
        audit(actor, "iam:org-type:update", "ORG_TYPE", Long.toString(typeId), null);
        return toVO(requireType(actor.tenantId(), typeId));
    }

    /** @param typeId 类型 ID @param request 状态参数 @return 修改后的类型 */
    @Transactional
    public OrganizationTypeVO changeStatus(long typeId, ChangeOrganizationTypeStatusRequest request) {
        TenantActorContext actor = actorService.requireRootOwner();
        TenantOrganizationMapper.OrganizationTypeRow current = requireType(actor.tenantId(), typeId);
        if (request.status().equals(current.status())) {
            return toVO(current);
        }
        if ("DISABLED".equals(request.status())
                && mapper.countOrganizationsUsingType(actor.tenantId(), typeId) > 0) {
            throw invalidRelation("正在被机构使用的类型不能停用");
        }
        if (mapper.updateOrganizationTypeStatus(
                actor.tenantId(), typeId, request.status(), request.version(), actor.accountId()) != 1) {
            throw conflict();
        }
        audit(actor, "iam:org-type:status", "ORG_TYPE", Long.toString(typeId), null);
        return toVO(requireType(actor.tenantId(), typeId));
    }

    /**
     * 整版替换允许关系，校验引用、去重、整图无环且不得破坏正在使用的机构父子关系。
     *
     * @param request 完整关系集合
     * @return 保存后的关系
     */
    @Transactional
    public List<OrganizationTypeRelationVO> replaceRelations(
            ReplaceOrganizationTypeRelationsRequest request
    ) {
        TenantActorContext actor = actorService.requireRootOwner();
        List<TenantOrganizationMapper.OrganizationTypeRow> activeTypes =
                mapper.selectActiveOrganizationTypes(actor.tenantId());
        Set<Long> activeTypeIds = activeTypes.stream()
                .map(TenantOrganizationMapper.OrganizationTypeRow::id)
                .collect(java.util.stream.Collectors.toSet());
        List<TenantOrganizationMapper.OrganizationTypePairRow> relations = new ArrayList<>();
        Set<String> relationKeys = new HashSet<>();
        for (OrganizationTypeRelationRequest relation : request.relations()) {
            if (relation.parentTypeId() == relation.childTypeId()
                    || !activeTypeIds.contains(relation.parentTypeId())
                    || !activeTypeIds.contains(relation.childTypeId())) {
                throw invalidRelation("允许关系只能引用不同的有效机构类型");
            }
            String key = relation.parentTypeId() + ":" + relation.childTypeId();
            if (!relationKeys.add(key)) {
                throw invalidRelation("机构类型允许关系不能重复");
            }
            relations.add(new TenantOrganizationMapper.OrganizationTypePairRow(
                    relation.parentTypeId(), relation.childTypeId()
            ));
        }
        validateAcyclic(activeTypeIds, relations);
        for (TenantOrganizationMapper.OrganizationTypePairRow used
                : mapper.selectUsedOrganizationTypeRelations(actor.tenantId())) {
            if (!relationKeys.contains(used.parentTypeId() + ":" + used.childTypeId())) {
                throw invalidRelation("不能删除机构树正在使用的类型父子关系");
            }
        }
        mapper.deactivateOrganizationTypeRelations(actor.tenantId(), actor.accountId());
        if (!relations.isEmpty()) {
            mapper.upsertOrganizationTypeRelations(actor.tenantId(), relations, actor.accountId());
        }
        audit(actor, "iam:org-type:relations", "TENANT", Long.toString(actor.tenantId()), null);
        return mapper.selectOrganizationTypeRelations(actor.tenantId()).stream()
                .map(this::toRelationVO)
                .toList();
    }

    /** 使用 Kahn 算法校验类型关系整图无环。 */
    private void validateAcyclic(
            Set<Long> typeIds,
            List<TenantOrganizationMapper.OrganizationTypePairRow> relations
    ) {
        Map<Long, Integer> indegrees = new HashMap<>();
        Map<Long, List<Long>> edges = new HashMap<>();
        typeIds.forEach(id -> indegrees.put(id, 0));
        for (TenantOrganizationMapper.OrganizationTypePairRow relation : relations) {
            edges.computeIfAbsent(relation.parentTypeId(), ignored -> new ArrayList<>())
                    .add(relation.childTypeId());
            indegrees.compute(relation.childTypeId(), (ignored, value) -> value == null ? 1 : value + 1);
        }
        ArrayDeque<Long> queue = new ArrayDeque<>();
        indegrees.forEach((id, degree) -> {
            if (degree == 0) queue.add(id);
        });
        int visited = 0;
        while (!queue.isEmpty()) {
            long current = queue.remove();
            visited++;
            for (long child : edges.getOrDefault(current, List.of())) {
                int degree = indegrees.computeIfPresent(child, (ignored, value) -> value - 1);
                if (degree == 0) queue.add(child);
            }
        }
        if (visited != typeIds.size()) {
            throw invalidRelation("机构类型允许关系不能形成环");
        }
    }

    /** @return 指定租户机构类型，不存在时抛出业务异常 */
    private TenantOrganizationMapper.OrganizationTypeRow requireType(long tenantId, long typeId) {
        TenantOrganizationMapper.OrganizationTypeRow row = mapper.selectOrganizationType(tenantId, typeId);
        if (row == null) {
            throw new BusinessException(TYPE_NOT_FOUND_CODE, "机构类型不存在", 404);
        }
        return row;
    }

    /** 写入成功审计。 */
    private void audit(TenantActorContext actor, String action, String targetType, String targetId, String reason) {
        auditLogService.recordTenantOrganizationAction(
                actor.tenantId(), actor.organizationId(), actor.accountId(), action,
                targetType, targetId, reason, "SUCCESS"
        );
    }

    /** @return 类型 VO */
    private OrganizationTypeVO toVO(TenantOrganizationMapper.OrganizationTypeRow row) {
        return new OrganizationTypeVO(
                Long.toString(row.id()), row.typeCode(), row.name(), row.sortOrder(), row.status(),
                row.sourceTemplateVersion(), row.version()
        );
    }

    /** @return 允许关系 VO */
    private OrganizationTypeRelationVO toRelationVO(
            TenantOrganizationMapper.OrganizationTypeRelationRow row
    ) {
        return new OrganizationTypeRelationVO(
                Long.toString(row.id()), Long.toString(row.parentTypeId()),
                Long.toString(row.childTypeId()), row.version()
        );
    }

    /** @return 乐观锁冲突异常 */
    private BusinessException conflict() {
        return new BusinessException(TYPE_CONFLICT_CODE, "机构类型状态已变化，请刷新后重试", 409);
    }

    /** @param message 错误消息 @return 非法关系异常 */
    private BusinessException invalidRelation(String message) {
        return new BusinessException(INVALID_RELATION_CODE, message, 400);
    }
}
