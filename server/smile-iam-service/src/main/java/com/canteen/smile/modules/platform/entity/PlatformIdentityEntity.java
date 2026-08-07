package com.canteen.smile.modules.platform.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `iam_platform_identity` 独立平台身份表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class PlatformIdentityEntity {

    /** 平台身份主键 ID。 */
    private Long id;

    /** 当前原始用户名。 */
    private String username;

    /** 归一化用户名。 */
    private String normalizedUsername;

    /** 可选显示名称。 */
    private String displayName;

    /** 平台身份状态。 */
    private String status;

    /** 授权版本。 */
    private Long authzVersion;

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
