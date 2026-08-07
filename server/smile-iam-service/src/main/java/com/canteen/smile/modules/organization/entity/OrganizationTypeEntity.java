package com.canteen.smile.modules.organization.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** `iam_org_type` 租户独立机构类型表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class OrganizationTypeEntity {

    /** 机构类型主键 ID。 */
    private Long id;

    /** 所属租户 ID。 */
    private Long tenantId;

    /** 租户内永久唯一类型编码。 */
    private String typeCode;

    /** 机构类型名称。 */
    private String name;

    /** 显示排序值。 */
    private Integer sortOrder;

    /** 类型状态。 */
    private String status;

    /** 来源模板版本，自定义类型为空。 */
    private Long sourceTemplateVersion;

    /** 创建账号 ID。 */
    private Long createdBy;

    /** 最后更新账号 ID。 */
    private Long updatedBy;
}
