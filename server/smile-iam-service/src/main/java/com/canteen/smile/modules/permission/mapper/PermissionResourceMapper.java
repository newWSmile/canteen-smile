package com.canteen.smile.modules.permission.mapper;

import com.canteen.smile.modules.permission.entity.PermissionResourceEntity;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/** 平台权限资源数据访问接口。 */
public interface PermissionResourceMapper {

    /** @return 权限资源分页总数 */
    long countResources(
            @Param("publishStatus") String publishStatus,
            @Param("appCode") String appCode,
            @Param("resourceType") String resourceType
    );

    /** @return 权限资源分页数据 */
    List<PermissionResourceRow> selectResources(
            @Param("publishStatus") String publishStatus,
            @Param("appCode") String appCode,
            @Param("resourceType") String resourceType,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** @return 指定权限资源，不存在时为空 */
    PermissionResourceRow selectResource(@Param("resourceId") long resourceId);

    /** @return 指定父资源，不存在时为空 */
    PermissionResourceRow selectParentResource(@Param("parentId") long parentId);

    /** @return 新增行数 */
    int insertResource(PermissionResourceEntity entity);

    /** @return 发布更新行数 */
    int publishResource(
            @Param("resourceId") long resourceId,
            @Param("version") long version,
            @Param("operatorId") long operatorId
    );

    /** @return 废弃更新行数 */
    int deprecateResource(
            @Param("resourceId") long resourceId,
            @Param("version") long version,
            @Param("operatorId") long operatorId
    );

    /** 为所有既有租户补齐新发布功能开关。 */
    int initializeFeatureForExistingTenants(
            @Param("featureCode") String featureCode,
            @Param("operatorId") long operatorId
    );

    /** 为所有既有租户补齐新发布菜单显示配置。 */
    int initializeMenuForExistingTenants(
            @Param("permissionId") long permissionId,
            @Param("operatorId") long operatorId
    );

    /**
     * 权限资源查询投影。
     *
     * @param id 资源 ID
     * @param permissionCode 权限码
     * @param resourceType 资源类型
     * @param parentId 父资源 ID
     * @param name 名称
     * @param description 说明
     * @param appCode 应用编码
     * @param routePath 前端路由
     * @param componentKey 组件键
     * @param apiMethod API 方法
     * @param apiPathPattern API 模板路径
     * @param featureCode 功能开关编码
     * @param publishStatus 发布状态
     * @param semanticVersion 语义版本
     * @param sortOrder 排序值
     * @param createdTime 创建时间
     * @param version 乐观锁版本
     */
    record PermissionResourceRow(
            long id,
            String permissionCode,
            String resourceType,
            Long parentId,
            String name,
            String description,
            String appCode,
            String routePath,
            String componentKey,
            String apiMethod,
            String apiPathPattern,
            String featureCode,
            String publishStatus,
            int semanticVersion,
            int sortOrder,
            OffsetDateTime createdTime,
            long version
    ) {
    }
}
