package com.canteen.smile.modules.organization.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** `iam_organization` 租户机构树节点表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class OrganizationEntity {

    /** 机构主键 ID。 */
    private Long id;

    /** 所属租户 ID。 */
    private Long tenantId;

    /** 父机构 ID。 */
    private Long parentId;

    /** 机构类型 ID。 */
    private Long organizationTypeId;

    /** 租户内永久唯一业务编码。 */
    private String businessCode;

    /** 机构名称。 */
    private String name;

    /** 同级唯一比较使用的归一化名称。 */
    private String normalizedName;

    /** 可选行政区域 ID。 */
    private Long adminRegionId;

    /** 机构自身状态。 */
    private String ownStatus;

    /** 创建账号 ID。 */
    private Long createdBy;

    /** 最后更新账号 ID。 */
    private Long updatedBy;
}
