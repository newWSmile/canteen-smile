package com.canteen.smile.modules.tenant.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** `iam_tenant_security_policy` 租户安全策略表实体。 */
@Getter
@Setter
@NoArgsConstructor
public class TenantSecurityPolicyEntity {

    /** 策略主键 ID。 */
    private Long id;
    /** 所属租户 ID。 */
    private Long tenantId;
    /** 是否允许同账号多设备并发登录。 */
    private Boolean concurrentLoginEnabled;
    /** 最大有效设备数。 */
    private Integer maxDevices;
    /** 是否允许记住我。 */
    private Boolean rememberMeEnabled;
    /** 普通会话空闲超时秒数。 */
    private Integer idleSeconds;
    /** 普通会话最长存活秒数。 */
    private Integer absoluteSeconds;
    /** 记住我会话空闲超时秒数。 */
    private Integer rememberIdleSeconds;
    /** 记住我会话最长存活秒数。 */
    private Integer rememberAbsoluteSeconds;
    /** 是否启用密码定期到期。 */
    private Boolean passwordExpiryEnabled;
    /** 密码有效天数。 */
    private Integer passwordExpiryDays;
    /** 审计保留天数。 */
    private Integer auditRetentionDays;
    /** 乐观锁版本。 */
    private Long version;
}
