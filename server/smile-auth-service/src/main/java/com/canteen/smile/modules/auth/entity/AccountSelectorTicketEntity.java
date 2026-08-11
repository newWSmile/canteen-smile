package com.canteen.smile.modules.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_account_selector_ticket` 手机号登录账号选择票据实体。 */
@Getter
@Setter
@NoArgsConstructor
public class AccountSelectorTicketEntity {

    /** 票据主键 ID。 */
    private Long id;

    /** 原始随机票据的 SHA-256 摘要。 */
    private String ticketHash;

    /** 已验证手机号的查询摘要。 */
    private String mobileHash;

    /** 签发时可登录账号 ID 集合摘要。 */
    private String candidateDigest;

    /** 应用入口编码。 */
    private String appCode;

    /** 票据状态。 */
    private String status;

    /** 绝对失效时间。 */
    private OffsetDateTime expiresAt;

    /** 一次性消费时间。 */
    private OffsetDateTime consumedTime;

    /** 乐观锁版本。 */
    private Long version;
}
