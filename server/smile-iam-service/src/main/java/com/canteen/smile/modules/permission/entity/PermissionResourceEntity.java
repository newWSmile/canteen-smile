package com.canteen.smile.modules.permission.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** `iam_permission_resource` 平台权限资源表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class PermissionResourceEntity {

    /** 权限资源主键。 */
    private Long id;
    /** 永久唯一权限码。 */
    private String permissionCode;
    /** 资源类型。 */
    private String resourceType;
    /** 可选父资源 ID。 */
    private Long parentId;
    /** 资源名称。 */
    private String name;
    /** 资源说明。 */
    private String description;
    /** 所属前端或服务应用。 */
    private String appCode;
    /** 前端路由路径。 */
    private String routePath;
    /** 前端本地组件键。 */
    private String componentKey;
    /** API HTTP 方法。 */
    private String apiMethod;
    /** API 模板路径。 */
    private String apiPathPattern;
    /** 功能开关编码。 */
    private String featureCode;
    /** 发布状态。 */
    private String publishStatus;
    /** 语义版本。 */
    private Integer semanticVersion;
    /** 同级排序值。 */
    private Integer sortOrder;
    /** 创建平台身份 ID。 */
    private Long createdBy;
    /** 最后更新平台身份 ID。 */
    private Long updatedBy;
    /** 乐观锁版本。 */
    private Long version;
}
