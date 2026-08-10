package com.canteen.smile.modules.sms.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_sms_challenge` 短信验证码挑战实体。 */
@Getter
@Setter
@NoArgsConstructor
public class SmsChallengeEntity {

    /** 挑战记录主键。 */
    private Long id;

    /** 对外随机挑战标识。 */
    private String challengeId;

    /** 验证码用途编码。 */
    private String purpose;

    /** 带服务端 Pepper 的手机号摘要。 */
    private String mobileHash;

    /** 带服务端 Pepper 的验证码摘要。 */
    private String codeHash;

    /** 真实供应商配置 ID；本地数据库日志策略为空。 */
    private Long providerConfigId;

    /** 真实供应商模板配置 ID；本地数据库日志策略为空。 */
    private Long templateConfigId;

    /** 累计校验失败次数。 */
    private Integer attempts;

    /** 挑战状态。 */
    private String status;

    /** 短信发送时间。 */
    private OffsetDateTime sentTime;

    /** 验证码失效时间。 */
    private OffsetDateTime expiresAt;

    /** 成功消费时间。 */
    private OffsetDateTime consumedTime;

    /** 乐观锁版本。 */
    private Long version;
}
