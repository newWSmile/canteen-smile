package com.canteen.smile.modules.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_credential` 认证凭证表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class CredentialEntity {

    /** 凭证主键 ID。 */
    private Long id;

    /** 认证主体类型。 */
    private String subjectType;

    /** 认证主体 ID。 */
    private Long subjectId;

    /** Argon2id 密码摘要。 */
    private String passwordHash;

    /** 密码哈希算法及参数版本。 */
    private String algorithm;

    /** 密码最近变更时间。 */
    private OffsetDateTime passwordChangedAt;

    /** 凭证安全版本。 */
    private Long credentialVersion;

    /** 凭证状态。 */
    private String status;

    /** 乐观锁版本。 */
    private Long version;
}
