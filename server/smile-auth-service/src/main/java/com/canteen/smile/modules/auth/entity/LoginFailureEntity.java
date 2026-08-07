package com.canteen.smile.modules.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_login_failure` 密码登录失败状态实体。 */
@Getter
@Setter
@NoArgsConstructor
public class LoginFailureEntity {

    /** 登录失败状态主键 ID。 */
    private Long id;

    /** 应用和用户名组合的不可逆摘要。 */
    private String subjectKeyHash;

    /** 最近来源 IP 摘要。 */
    private String lastIpHash;

    /** 最近设备标识摘要。 */
    private String lastDeviceHash;

    /** 当前连续密码失败次数。 */
    private Integer passwordFailures;

    /** 是否要求图形验证码。 */
    private Boolean captchaRequired;

    /** 密码登录锁定截止时间。 */
    private OffsetDateTime lockedUntil;

    /** 最近失败时间。 */
    private OffsetDateTime lastFailureTime;

    /** 乐观锁版本。 */
    private Long version;
}
