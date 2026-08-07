package com.canteen.smile.modules.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** `auth_platform_recovery_code` 平台一次性恢复码表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class PlatformRecoveryCodeEntity {

    /** 恢复码记录主键 ID。 */
    private Long id;

    /** 平台身份 ID。 */
    private Long platformIdentityId;

    /** 恢复码生成批次 ID。 */
    private String batchId;

    /** 恢复码 SHA-256 摘要。 */
    private String codeHash;

    /** 恢复码状态。 */
    private String status;

    /** 乐观锁版本。 */
    private Long version;
}
