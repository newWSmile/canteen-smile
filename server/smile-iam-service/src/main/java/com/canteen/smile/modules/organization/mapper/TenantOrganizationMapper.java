package com.canteen.smile.modules.organization.mapper;

import com.canteen.smile.modules.organization.entity.OrganizationEntity;
import com.canteen.smile.modules.organization.entity.OrganizationTypeEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 租户机构类型、允许关系和机构树数据访问接口。 */
public interface TenantOrganizationMapper {

    /** @return 下一个机构主键 ID */
    long nextOrganizationId();

    /** @param tenantId 租户 ID @param status 可选状态 @return 类型数量 */
    long countOrganizationTypes(@Param("tenantId") long tenantId, @Param("status") String status);

    /** @return 当前分页的机构类型 */
    List<OrganizationTypeRow> selectOrganizationTypes(
            @Param("tenantId") long tenantId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** @param tenantId 租户 ID @return 全部有效机构类型 */
    List<OrganizationTypeRow> selectActiveOrganizationTypes(@Param("tenantId") long tenantId);

    /** @return 指定租户机构类型 */
    OrganizationTypeRow selectOrganizationType(
            @Param("tenantId") long tenantId,
            @Param("typeId") long typeId
    );

    /** @param entity 新机构类型 @return 新增行数 */
    int insertOrganizationType(OrganizationTypeEntity entity);

    /** @return 修改机构类型名称和排序的行数 */
    int updateOrganizationType(
            @Param("tenantId") long tenantId,
            @Param("typeId") long typeId,
            @Param("name") String name,
            @Param("sortOrder") int sortOrder,
            @Param("version") long version,
            @Param("operatorId") long operatorId
    );

    /** @return 修改机构类型状态的行数 */
    int updateOrganizationTypeStatus(
            @Param("tenantId") long tenantId,
            @Param("typeId") long typeId,
            @Param("status") String status,
            @Param("version") long version,
            @Param("operatorId") long operatorId
    );

    /** @param tenantId 租户 ID @return 当前有效允许关系 */
    List<OrganizationTypeRelationRow> selectOrganizationTypeRelations(@Param("tenantId") long tenantId);

    /** @param tenantId 租户 ID @return 实际机构树正在使用的类型边集合 */
    List<OrganizationTypePairRow> selectUsedOrganizationTypeRelations(@Param("tenantId") long tenantId);

    /** @return 将现有允许关系全部置为逻辑删除的行数 */
    int deactivateOrganizationTypeRelations(
            @Param("tenantId") long tenantId,
            @Param("operatorId") long operatorId
    );

    /** @return 批量新增或恢复允许关系的行数 */
    int upsertOrganizationTypeRelations(
            @Param("tenantId") long tenantId,
            @Param("relations") List<OrganizationTypePairRow> relations,
            @Param("operatorId") long operatorId
    );

    /** @return 是否存在有效父子类型允许关系 */
    long countActiveTypeRelation(
            @Param("tenantId") long tenantId,
            @Param("parentTypeId") long parentTypeId,
            @Param("childTypeId") long childTypeId
    );

    /** @param tenantId 租户 ID @param typeId 类型 ID @return 使用该类型的机构数量 */
    long countOrganizationsUsingType(@Param("tenantId") long tenantId, @Param("typeId") long typeId);

    /** @return 指定机构树节点 */
    OrganizationRow selectOrganization(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId
    );

    /** @return 直属子机构数量 */
    long countChildOrganizations(
            @Param("tenantId") long tenantId,
            @Param("parentId") long parentId
    );

    /** @return 当前页直属子机构 */
    List<OrganizationRow> selectChildOrganizations(
            @Param("tenantId") long tenantId,
            @Param("parentId") long parentId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** @return 匹配关键词的机构搜索结果，SQL 内限制返回数量 */
    List<OrganizationSearchRow> searchOrganizations(
            @Param("tenantId") long tenantId,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    /** @param entity 新机构节点 @return 新增行数 */
    int insertOrganization(OrganizationEntity entity);

    /** @return 永久占用机构业务编码的行数 */
    int insertOrganizationCodeRegistry(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("normalizedCode") String normalizedCode,
            @Param("operatorId") long operatorId
    );

    /** @return 新机构继承父路径并写入自身闭包的行数 */
    int insertOrganizationClosure(
            @Param("tenantId") long tenantId,
            @Param("parentId") long parentId,
            @Param("organizationId") long organizationId,
            @Param("operatorId") long operatorId
    );

    /** @return 修改机构资料的行数 */
    int updateOrganization(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("organizationTypeId") long organizationTypeId,
            @Param("name") String name,
            @Param("normalizedName") String normalizedName,
            @Param("adminRegionId") Long adminRegionId,
            @Param("version") long version,
            @Param("operatorId") long operatorId
    );

    /** @return 机构名称历史新增行数 */
    int insertOrganizationNameHistory(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("oldName") String oldName,
            @Param("newName") String newName,
            @Param("operatorId") long operatorId
    );

    /** @param organizationId 机构 ID @return 直属子机构类型 ID */
    List<Long> selectChildOrganizationTypeIds(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId
    );

    /** @return 祖先到后代的有效闭包路径数量 */
    long countOrganizationPath(
            @Param("tenantId") long tenantId,
            @Param("ancestorId") long ancestorId,
            @Param("descendantId") long descendantId
    );

    /** @return 迁移机构父节点的行数 */
    int moveOrganization(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("newParentId") long newParentId,
            @Param("version") long version,
            @Param("operatorId") long operatorId
    );

    /** @return 迁移前逻辑删除旧外部祖先路径的行数 */
    int deactivateOldOrganizationPaths(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("operatorId") long operatorId
    );

    /** @return 迁移后写入新祖先路径的行数 */
    int insertMovedOrganizationPaths(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("newParentId") long newParentId,
            @Param("operatorId") long operatorId
    );

    /** @return 修改机构自身状态的行数 */
    int updateOrganizationStatus(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("status") String status,
            @Param("version") long version,
            @Param("operatorId") long operatorId
    );

    /** @return 阻止空白机构删除的依赖记录总数 */
    long countOrganizationDeleteDependencies(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId
    );

    /** @return 逻辑删除空白机构闭包记录的行数 */
    int deleteOrganizationClosures(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("operatorId") long operatorId
    );

    /** @return 逻辑删除空白机构节点的行数 */
    int deleteOrganization(
            @Param("tenantId") long tenantId,
            @Param("organizationId") long organizationId,
            @Param("version") long version,
            @Param("operatorId") long operatorId
    );

    /** @param adminRegionId 行政区域 ID @return 有效行政区域数量 */
    long countActiveAdminRegion(@Param("adminRegionId") long adminRegionId);

    /** 机构类型数据库行。 */
    record OrganizationTypeRow(
            long id,
            String typeCode,
            String name,
            int sortOrder,
            String status,
            Long sourceTemplateVersion,
            long version
    ) {
    }

    /** 机构类型允许关系数据库行。 */
    record OrganizationTypeRelationRow(long id, long parentTypeId, long childTypeId, long version) {
    }

    /** 机构类型父子 ID 对，同时用于批量关系写入。 */
    record OrganizationTypePairRow(long parentTypeId, long childTypeId) {
    }

    /** 机构树节点数据库行。 */
    record OrganizationRow(
            long id,
            Long parentId,
            long organizationTypeId,
            String typeCode,
            String typeName,
            String businessCode,
            String name,
            Long adminRegionId,
            String ownStatus,
            String effectiveStatus,
            long pathVersion,
            boolean hasChildren,
            long version
    ) {
    }

    /** 机构搜索结果数据库行。 */
    record OrganizationSearchRow(
            long id,
            Long parentId,
            long organizationTypeId,
            String typeName,
            String businessCode,
            String name,
            String effectiveStatus,
            String breadcrumb
    ) {
    }
}
