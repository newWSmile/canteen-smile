package com.canteen.smile.modules.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_reauth_ticket` 敏感操作再认证票据摘要实体。 */
@Getter
@Setter
@NoArgsConstructor
public class ReauthTicketEntity {

    /** 票据记录主键。 */
    private Long id;

    /** 完成再认证的主体类型。 */
    private String subjectType;

    /** 完成再认证的主体 ID。 */
    private Long subjectId;

    /** 原始随机票据的 SHA-256 摘要。 */
    private String ticketHash;

    /** 票据允许执行的唯一敏感操作。 */
    private String allowedAction;

    /** 再认证方式。 */
    private String verifyMethod;

    /** 票据状态。 */
    private String status;

    /** 票据绝对失效时间。 */
    private OffsetDateTime expiresAt;

    /** 消费时间。 */
    private OffsetDateTime consumedTime;

    /** 乐观锁版本。 */
    private Long version;
}
