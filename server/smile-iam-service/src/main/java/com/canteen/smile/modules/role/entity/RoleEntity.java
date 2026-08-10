package com.canteen.smile.modules.role.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** `iam_role` 机构角色表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class RoleEntity {

    /** 角色主键。 */
    private Long id;
    /** 所属租户。 */
    private Long tenantId;
    /** 所属机构。 */
    private Long organizationId;
    /** 永久唯一系统角色编码。 */
    private String roleCode;
    /** 角色名称。 */
    private String name;
    /** 归一化角色名称。 */
    private String normalizedName;
    /** 角色说明。 */
    private String description;
    /** OWNER 或 CUSTOM。 */
    private String roleType;
    /** ACTIVE 或 DISABLED。 */
    private String status;
    /** 授权版本。 */
    private Long authzVersion;
    /** 创建账号。 */
    private Long createdBy;
    /** 更新账号。 */
    private Long updatedBy;
    /** 乐观锁版本。 */
    private Long version;
}
