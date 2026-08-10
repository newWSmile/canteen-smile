package com.canteen.smile.modules.sms.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_sms_runtime_policy` 全局短信运行策略实体。 */
@Getter
@Setter
public class SmsRuntimePolicyEntity {

    /** 主键 ID。 */
    private Long id;
    /** 固定全局策略编码。 */
    private String policyCode;
    /** 验证码有效秒数。 */
    private Integer challengeTtlSeconds;
    /** 同手机号重发等待秒数。 */
    private Integer resendIntervalSeconds;
    /** 最大错误校验次数。 */
    private Integer maxVerificationAttempts;
    /** 手机号小时限额。 */
    private Integer mobileHourlyLimit;
    /** 手机号每日限额。 */
    private Integer mobileDailyLimit;
    /** IP 小时限额。 */
    private Integer ipHourlyLimit;
    /** IP 每日限额。 */
    private Integer ipDailyLimit;
    /** 设备小时限额。 */
    private Integer deviceHourlyLimit;
    /** 设备每日限额。 */
    private Integer deviceDailyLimit;
    /** 是否保留验证码明文正文。 */
    private Boolean plaintextCodeRetentionEnabled;
    /** 创建时间。 */
    private OffsetDateTime createdTime;
    /** 更新时间。 */
    private OffsetDateTime updatedTime;
    /** 乐观锁版本。 */
    private Long version;
}
