package com.canteen.smile.modules.permission.service;

import com.canteen.smile.common.api.PageResult;
import com.canteen.smile.common.exception.BusinessException;
import com.canteen.smile.modules.audit.service.IamAuditLogService;
import com.canteen.smile.modules.permission.dto.CreatePermissionResourceRequest;
import com.canteen.smile.modules.permission.dto.PermissionResourcePageQuery;
import com.canteen.smile.modules.permission.entity.PermissionResourceEntity;
import com.canteen.smile.modules.permission.mapper.PermissionResourceMapper;
import com.canteen.smile.modules.permission.vo.PermissionResourceVO;
import com.canteen.smile.modules.platform.service.PlatformActorService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 平台权限资源草稿、发布和永久废弃服务。 */
@Service
@RequiredArgsConstructor
public class PermissionResourceService {

    /** 权限资源数据访问接口。 */
    private final PermissionResourceMapper mapper;
    /** 平台操作人解析服务。 */
    private final PlatformActorService actorService;
    /** IAM 审计服务。 */
    private final IamAuditLogService auditLogService;

    /** @param query 分页条件 @return 权限资源分页 */
    @Transactional(readOnly = true)
    public PageResult<PermissionResourceVO> page(PermissionResourcePageQuery query) {
        long total = mapper.countResources(query.getPublishStatus(), query.getAppCode(), query.getResourceType());
        List<PermissionResourceVO> items = mapper.selectResources(
                query.getPublishStatus(), query.getAppCode(), query.getResourceType(),
                (query.getPageNo() - 1) * query.getPageSize(), query.getPageSize()
        ).stream().map(this::toVO).toList();
        return new PageResult<>(items, query.getPageNo(), query.getPageSize(), total);
    }

    /** @param request 草稿参数 @return 新建权限资源 */
    @Transactional
    public PermissionResourceVO create(CreatePermissionResourceRequest request) {
        long operatorId = actorService.currentPlatformIdentityId();
        Long parentId = parseNullableId(request.parentId());
        validateResourceShape(request, parentId);
        PermissionResourceEntity entity = new PermissionResourceEntity();
        entity.setPermissionCode(request.permissionCode().strip());
        entity.setResourceType(request.resourceType());
        entity.setParentId(parentId);
        entity.setName(request.name().strip());
        entity.setDescription(trimToNull(request.description()));
        entity.setAppCode(request.appCode());
        entity.setRoutePath(trimToNull(request.routePath()));
        entity.setComponentKey(trimToNull(request.componentKey()));
        entity.setApiMethod(request.apiMethod());
        entity.setApiPathPattern(trimToNull(request.apiPathPattern()));
        entity.setFeatureCode(trimToNull(request.featureCode()));
        entity.setSemanticVersion(request.semanticVersion());
        entity.setSortOrder(request.sortOrder());
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        try {
            mapper.insertResource(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("IAM_2601", "权限码或 API 模板路径已被永久占用", 409);
        }
        auditLogService.recordPlatformAction(
                operatorId, "platform:permission:create", "新建权限资源草稿", "PERMISSION_RESOURCE",
                entity.getId().toString(), "SUCCESS"
        );
        return toVO(requireResource(entity.getId()));
    }

    /** @param resourceId 资源 ID @param version 乐观锁版本 @return 已发布资源 */
    @Transactional
    public PermissionResourceVO publish(long resourceId, long version) {
        long operatorId = actorService.currentPlatformIdentityId();
        PermissionResourceMapper.PermissionResourceRow current = requireResource(resourceId);
        if (!"DRAFT".equals(current.publishStatus())) {
            throw new BusinessException("IAM_2602", "只有草稿权限资源可以发布", 409);
        }
        if (mapper.publishResource(resourceId, version, operatorId) != 1) {
            throw conflict();
        }
        if (current.featureCode() != null) {
            mapper.initializeFeatureForExistingTenants(current.featureCode(), operatorId);
        }
        if ("MENU".equals(current.resourceType())
                && ("TENANT_ADMIN".equals(current.appCode()) || "TENANT_PORTAL".equals(current.appCode()))) {
            mapper.initializeMenuForExistingTenants(resourceId, operatorId);
        }
        auditLogService.recordPlatformAction(
                operatorId, "platform:permission:publish", "发布权限资源", "PERMISSION_RESOURCE",
                Long.toString(resourceId), "SUCCESS"
        );
        return toVO(requireResource(resourceId));
    }

    /** @param resourceId 资源 ID @param version 乐观锁版本 @return 已废弃资源 */
    @Transactional
    public PermissionResourceVO deprecate(long resourceId, long version) {
        long operatorId = actorService.currentPlatformIdentityId();
        PermissionResourceMapper.PermissionResourceRow current = requireResource(resourceId);
        if (!"PUBLISHED".equals(current.publishStatus())) {
            throw new BusinessException("IAM_2602", "只有已发布权限资源可以废弃", 409);
        }
        if (mapper.deprecateResource(resourceId, version, operatorId) != 1) {
            throw conflict();
        }
        auditLogService.recordPlatformAction(
                operatorId, "platform:permission:deprecate", "废弃权限资源", "PERMISSION_RESOURCE",
                Long.toString(resourceId), "SUCCESS"
        );
        return toVO(requireResource(resourceId));
    }

    /** 校验不同资源类型的真实字段组合以及父节点边界。 */
    private void validateResourceShape(CreatePermissionResourceRequest request, Long parentId) {
        if ("API".equals(request.resourceType())
                && (request.apiMethod() == null || trimToNull(request.apiPathPattern()) == null)) {
            throw new BusinessException("IAM_2603", "API 资源必须填写 HTTP 方法和模板路径", 400);
        }
        if (!"API".equals(request.resourceType())
                && (request.apiMethod() != null || trimToNull(request.apiPathPattern()) != null)) {
            throw new BusinessException("IAM_2603", "只有 API 资源可以填写 HTTP 方法和模板路径", 400);
        }
        if (parentId == null) return;
        PermissionResourceMapper.PermissionResourceRow parent = mapper.selectParentResource(parentId);
        if (parent == null || !request.appCode().equals(parent.appCode())
                || !("DIRECTORY".equals(parent.resourceType()) || "MENU".equals(parent.resourceType()))) {
            throw new BusinessException("IAM_2603", "父资源必须是同一应用内的目录或菜单", 400);
        }
    }

    /** @return 指定权限资源 */
    private PermissionResourceMapper.PermissionResourceRow requireResource(long resourceId) {
        PermissionResourceMapper.PermissionResourceRow row = mapper.selectResource(resourceId);
        if (row == null) throw new BusinessException("IAM_2604", "权限资源不存在", 404);
        return row;
    }

    /** @return 权限资源响应 */
    private PermissionResourceVO toVO(PermissionResourceMapper.PermissionResourceRow row) {
        return new PermissionResourceVO(
                Long.toString(row.id()), row.permissionCode(), row.resourceType(),
                row.parentId() == null ? null : Long.toString(row.parentId()), row.name(), row.description(),
                row.appCode(), row.routePath(), row.componentKey(), row.apiMethod(), row.apiPathPattern(),
                row.featureCode(), row.publishStatus(), row.semanticVersion(), row.sortOrder(),
                row.createdTime(), row.version()
        );
    }

    /** @return 可空文本 ID 转换结果 */
    private Long parseNullableId(String value) {
        return value == null ? null : Long.parseLong(value);
    }

    /** @return 去空白后的可空文本 */
    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** @return 权限资源状态冲突异常 */
    private BusinessException conflict() {
        return new BusinessException("IAM_2605", "权限资源状态已变化，请刷新后重试", 409);
    }
}
