package com.canteen.smile.modules.organization.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.account.service.TenantActorContext;
import com.canteen.smile.modules.account.service.TenantActorService;
import com.canteen.smile.modules.audit.service.IamAuditLogService;
import com.canteen.smile.modules.organization.dto.ChangeOrganizationStatusRequest;
import com.canteen.smile.modules.organization.dto.CreateOrganizationRequest;
import com.canteen.smile.modules.organization.dto.DeleteOrganizationRequest;
import com.canteen.smile.modules.organization.dto.MoveOrganizationRequest;
import com.canteen.smile.modules.organization.dto.OrganizationPageQuery;
import com.canteen.smile.modules.organization.dto.UpdateOrganizationRequest;
import com.canteen.smile.modules.organization.entity.OrganizationEntity;
import com.canteen.smile.modules.organization.mapper.TenantOrganizationMapper;
import com.canteen.smile.modules.organization.vo.OrganizationSearchVO;
import com.canteen.smile.modules.organization.vo.OrganizationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** 租户机构树查询与变更服务。 */
@Service
@RequiredArgsConstructor
public class TenantOrganizationService {

    /** 机构不存在错误码。 */
    private static final String ORGANIZATION_NOT_FOUND_CODE = "IAM_2510";

    /** 机构父子结构或引用不合法错误码。 */
    private static final String INVALID_ORGANIZATION_CODE = "IAM_2511";

    /** 机构唯一约束或乐观锁冲突错误码。 */
    private static final String ORGANIZATION_CONFLICT_CODE = "IAM_2512";

    /** 非空机构不允许删除错误码。 */
    private static final String ORGANIZATION_NOT_EMPTY_CODE = "IAM_2513";

    /** 机构搜索最大返回数量。 */
    private static final int MAX_SEARCH_RESULT = 20;

    /** 当前租户操作人服务。 */
    private final TenantActorService actorService;

    /** 机构治理数据访问接口。 */
    private final TenantOrganizationMapper mapper;

    /** IAM 管理审计服务。 */
    private final IamAuditLogService auditLogService;

    /** @return 当前租户根机构 */
    @Transactional(readOnly = true)
    public OrganizationVO root() {
        TenantActorContext actor = actorService.current();
        return toVO(requireOrganization(actor.tenantId(), actor.rootOrganizationId()));
    }

    /** @param organizationId 机构 ID @return 当前租户内的机构详情 */
    @Transactional(readOnly = true)
    public OrganizationVO detail(long organizationId) {
        TenantActorContext actor = actorService.current();
        return toVO(requireOrganization(actor.tenantId(), organizationId));
    }

    /** @param query 直属子节点分页条件 @return 当前页机构 */
    @Transactional(readOnly = true)
    public PageResult<OrganizationVO> children(OrganizationPageQuery query) {
        TenantActorContext actor = actorService.current();
        requireOrganization(actor.tenantId(), query.getParentId());
        long total = mapper.countChildOrganizations(actor.tenantId(), query.getParentId());
        List<OrganizationVO> items = mapper.selectChildOrganizations(
                actor.tenantId(), query.getParentId(),
                (query.getPageNo() - 1) * query.getPageSize(), query.getPageSize()
        ).stream().map(this::toVO).toList();
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), total);
    }

    /** @param keyword 名称或业务编码关键词 @return 最多二十个当前租户机构 */
    @Transactional(readOnly = true)
    public List<OrganizationSearchVO> search(String keyword) {
        TenantActorContext actor = actorService.current();
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword.length() < 2) {
            throw invalid("机构搜索关键词至少需要两个字符");
        }
        return mapper.searchOrganizations(actor.tenantId(), normalizedKeyword, MAX_SEARCH_RESULT)
                .stream().map(this::toSearchVO).toList();
    }

    /** @param request 新机构参数 @return 新增机构 */
    @Transactional
    public OrganizationVO create(CreateOrganizationRequest request) {
        TenantActorContext actor = actorService.current();
        TenantOrganizationMapper.OrganizationRow parent =
                requireOrganization(actor.tenantId(), request.parentId());
        if (!"ACTIVE".equals(parent.effectiveStatus())) {
            throw invalid("停用机构下不能新增子机构");
        }
        requireActiveType(actor.tenantId(), request.organizationTypeId());
        requireAllowedRelation(actor.tenantId(), parent.organizationTypeId(), request.organizationTypeId());
        validateAdminRegion(request.adminRegionId());

        long organizationId = mapper.nextOrganizationId();
        OrganizationEntity entity = new OrganizationEntity();
        entity.setId(organizationId);
        entity.setTenantId(actor.tenantId());
        entity.setParentId(parent.id());
        entity.setOrganizationTypeId(request.organizationTypeId());
        entity.setBusinessCode(request.businessCode().strip());
        entity.setName(request.name().strip());
        entity.setNormalizedName(normalize(request.name()));
        entity.setAdminRegionId(request.adminRegionId());
        entity.setOwnStatus("ACTIVE");
        entity.setCreatedBy(actor.accountId());
        entity.setUpdatedBy(actor.accountId());
        try {
            mapper.insertOrganization(entity);
            mapper.insertOrganizationCodeRegistry(
                    actor.tenantId(), organizationId, normalize(request.businessCode()), actor.accountId()
            );
            mapper.insertOrganizationClosure(
                    actor.tenantId(), parent.id(), organizationId, actor.accountId()
            );
        } catch (DuplicateKeyException exception) {
            throw conflict("机构业务编码已被永久占用，或同一父机构下已存在同名机构");
        }
        audit(actor, "iam:org:create", organizationId, null);
        return toVO(requireOrganization(actor.tenantId(), organizationId));
    }

    /** @param organizationId 机构 ID @param request 修改参数 @return 修改后的机构 */
    @Transactional
    public OrganizationVO update(long organizationId, UpdateOrganizationRequest request) {
        TenantActorContext actor = actorService.current();
        TenantOrganizationMapper.OrganizationRow current =
                requireOrganization(actor.tenantId(), organizationId);
        requireActiveType(actor.tenantId(), request.organizationTypeId());
        if (current.parentId() != null) {
            TenantOrganizationMapper.OrganizationRow parent =
                    requireOrganization(actor.tenantId(), current.parentId());
            requireAllowedRelation(actor.tenantId(), parent.organizationTypeId(), request.organizationTypeId());
        }
        for (Long childTypeId : mapper.selectChildOrganizationTypeIds(actor.tenantId(), organizationId)) {
            requireAllowedRelation(actor.tenantId(), request.organizationTypeId(), childTypeId);
        }
        validateAdminRegion(request.adminRegionId());
        String newName = request.name().strip();
        try {
            if (mapper.updateOrganization(
                    actor.tenantId(), organizationId, request.organizationTypeId(), newName,
                    normalize(newName), request.adminRegionId(), request.version(), actor.accountId()
            ) != 1) {
                throw conflict("机构资料已变化，请刷新后重试");
            }
        } catch (DuplicateKeyException exception) {
            throw conflict("同一父机构下已存在同名机构");
        }
        if (!current.name().equals(newName)) {
            mapper.insertOrganizationNameHistory(
                    actor.tenantId(), organizationId, current.name(), newName, actor.accountId()
            );
        }
        audit(actor, "iam:org:update", organizationId, null);
        return toVO(requireOrganization(actor.tenantId(), organizationId));
    }

    /** @param organizationId 机构 ID @param request 迁移参数 @return 迁移后的机构 */
    @Transactional
    public OrganizationVO move(long organizationId, MoveOrganizationRequest request) {
        TenantActorContext actor = actorService.current();
        TenantOrganizationMapper.OrganizationRow current =
                requireOrganization(actor.tenantId(), organizationId);
        if (current.parentId() == null) {
            throw invalid("租户根机构不能迁移");
        }
        TenantOrganizationMapper.OrganizationRow newParent =
                requireOrganization(actor.tenantId(), request.newParentId());
        if (!"ACTIVE".equals(newParent.effectiveStatus())) {
            throw invalid("不能迁移到已停用的机构下");
        }
        if (organizationId == request.newParentId()
                || mapper.countOrganizationPath(actor.tenantId(), organizationId, request.newParentId()) > 0) {
            throw invalid("新父机构不能是当前机构或其下级机构");
        }
        requireAllowedRelation(actor.tenantId(), newParent.organizationTypeId(), current.organizationTypeId());
        try {
            if (mapper.moveOrganization(
                    actor.tenantId(), organizationId, request.newParentId(), request.version(), actor.accountId()
            ) != 1) {
                throw conflict("机构路径已变化，请刷新后重试");
            }
            mapper.deactivateOldOrganizationPaths(actor.tenantId(), organizationId, actor.accountId());
            mapper.insertMovedOrganizationPaths(
                    actor.tenantId(), organizationId, request.newParentId(), actor.accountId()
            );
        } catch (DuplicateKeyException exception) {
            throw conflict("新父机构下已存在同名机构，或机构路径发生冲突");
        }
        audit(actor, "iam:org:move", organizationId, request.reason().strip());
        return toVO(requireOrganization(actor.tenantId(), organizationId));
    }

    /** @param organizationId 机构 ID @param request 状态参数 @return 修改后的机构 */
    @Transactional
    public OrganizationVO changeStatus(long organizationId, ChangeOrganizationStatusRequest request) {
        TenantActorContext actor = actorService.current();
        TenantOrganizationMapper.OrganizationRow current =
                requireOrganization(actor.tenantId(), organizationId);
        if (current.parentId() == null) {
            throw invalid("租户根机构状态由平台租户生命周期管理");
        }
        if (request.status().equals(current.ownStatus())) {
            return toVO(current);
        }
        if (mapper.updateOrganizationStatus(
                actor.tenantId(), organizationId, request.status(), request.version(), actor.accountId()
        ) != 1) {
            throw conflict("机构状态已变化，请刷新后重试");
        }
        audit(actor, "iam:org:status", organizationId, request.reason().strip());
        return toVO(requireOrganization(actor.tenantId(), organizationId));
    }

    /** @param organizationId 机构 ID @param request 删除参数 */
    @Transactional
    public void delete(long organizationId, DeleteOrganizationRequest request) {
        TenantActorContext actor = actorService.current();
        TenantOrganizationMapper.OrganizationRow current =
                requireOrganization(actor.tenantId(), organizationId);
        if (current.parentId() == null) {
            throw invalid("租户根机构不能删除");
        }
        if (mapper.countOrganizationDeleteDependencies(actor.tenantId(), organizationId) > 0) {
            throw new BusinessException(
                    ORGANIZATION_NOT_EMPTY_CODE,
                    "机构存在子机构、账号、角色或所有者绑定，只能停用，不能删除",
                    409
            );
        }
        mapper.deleteOrganizationClosures(actor.tenantId(), organizationId, actor.accountId());
        if (mapper.deleteOrganization(
                actor.tenantId(), organizationId, request.version(), actor.accountId()
        ) != 1) {
            throw conflict("机构状态已变化，请刷新后重试");
        }
        audit(actor, "iam:org:delete", organizationId, request.reason().strip());
    }

    /** 校验机构类型有效。 */
    private void requireActiveType(long tenantId, long typeId) {
        TenantOrganizationMapper.OrganizationTypeRow type = mapper.selectOrganizationType(tenantId, typeId);
        if (type == null || !"ACTIVE".equals(type.status())) {
            throw invalid("所选机构类型不存在或已停用");
        }
    }

    /** 校验父子类型关系已经由租户配置允许。 */
    private void requireAllowedRelation(long tenantId, long parentTypeId, long childTypeId) {
        if (mapper.countActiveTypeRelation(tenantId, parentTypeId, childTypeId) == 0) {
            throw invalid("当前机构类型关系不允许该父子结构");
        }
    }

    /** 校验可选行政区域引用。 */
    private void validateAdminRegion(Long adminRegionId) {
        if (adminRegionId != null && mapper.countActiveAdminRegion(adminRegionId) == 0) {
            throw invalid("所选行政区域不存在或已停用");
        }
    }

    /** @return 指定租户机构，不存在时抛出业务异常 */
    private TenantOrganizationMapper.OrganizationRow requireOrganization(long tenantId, long organizationId) {
        TenantOrganizationMapper.OrganizationRow row = mapper.selectOrganization(tenantId, organizationId);
        if (row == null) {
            throw new BusinessException(ORGANIZATION_NOT_FOUND_CODE, "机构不存在", 404);
        }
        return row;
    }

    /** 写入成功审计。 */
    private void audit(TenantActorContext actor, String action, long targetId, String reason) {
        auditLogService.recordTenantOrganizationAction(
                actor.tenantId(), actor.accountId(), action, "ORGANIZATION",
                Long.toString(targetId), reason, "SUCCESS"
        );
    }

    /** @return Unicode NFKC、去首尾空白并转小写后的比较值 */
    private String normalize(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    /** @return 机构 VO */
    private OrganizationVO toVO(TenantOrganizationMapper.OrganizationRow row) {
        return new OrganizationVO(
                Long.toString(row.id()), nullableId(row.parentId()),
                Long.toString(row.organizationTypeId()), row.typeCode(), row.typeName(),
                row.businessCode(), row.name(), nullableId(row.adminRegionId()), row.ownStatus(),
                row.effectiveStatus(), row.pathVersion(), row.hasChildren(), row.version()
        );
    }

    /** @return 机构搜索 VO */
    private OrganizationSearchVO toSearchVO(TenantOrganizationMapper.OrganizationSearchRow row) {
        return new OrganizationSearchVO(
                Long.toString(row.id()), nullableId(row.parentId()),
                Long.toString(row.organizationTypeId()), row.typeName(), row.businessCode(),
                row.name(), row.effectiveStatus(), row.breadcrumb()
        );
    }

    /** @return 可空 bigint 的 JSON 字符串表达 */
    private String nullableId(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /** @return 非法机构结构异常 */
    private BusinessException invalid(String message) {
        return new BusinessException(INVALID_ORGANIZATION_CODE, message, 400);
    }

    /** @return 机构冲突异常 */
    private BusinessException conflict(String message) {
        return new BusinessException(ORGANIZATION_CONFLICT_CODE, message, 409);
    }
}
