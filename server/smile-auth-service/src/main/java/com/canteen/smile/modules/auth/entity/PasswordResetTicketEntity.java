package com.canteen.smile.modules.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_password_reset_ticket` 一次性密码重置票据摘要实体。 */
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetTicketEntity {

    /** 票据记录主键。 */
    private Long id;

    /** 认证主体类型。 */
    private String subjectType;

    /** 认证主体 ID。 */
    private Long subjectId;

    /** 重置模式。 */
    private String resetMode;

    /** 原始随机票据的 SHA-256 摘要。 */
    private String ticketHash;

    /** 发起人类型。 */
    private String initiatedByType;

    /** 发起人 ID。 */
    private Long initiatedById;

    /** 票据状态。 */
    private String status;

    /** 票据绝对失效时间。 */
    private OffsetDateTime expiresAt;

    /** 消费时间。 */
    private OffsetDateTime consumedTime;

    /** 乐观锁版本。 */
    private Long version;
}
