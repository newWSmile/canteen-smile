package com.canteen.smile.modules.tenant.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `iam_tenant` 租户隔离边界表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class TenantEntity {

    /** 租户主键 ID。 */
    private Long id;

    /** 永久唯一的租户业务编码。 */
    private String tenantCode;

    /** 租户名称。 */
    private String name;

    /** 租户生命周期状态。 */
    private String status;

    /** 租户根机构 ID。 */
    private Long rootOrganizationId;

    /** 租户安全版本。 */
    private Long securityVersion;

    /** 租户初始化使用的机构类型模板版本。 */
    private Long templateVersion;

    /** Auth 初始化编排状态。 */
    private String provisionStatus;

    /** 创建者平台身份 ID。 */
    private Long createdBy;

    /** 创建时间。 */
    private OffsetDateTime createdTime;

    /** 最后更新者平台身份 ID。 */
    private Long updatedBy;

    /** 最后更新时间。 */
    private OffsetDateTime updatedTime;

    /** 逻辑删除标记。 */
    private Boolean deleted;

    /** 乐观锁版本。 */
    private Long version;

}
