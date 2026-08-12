package com.canteen.smile.modules.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_device_session` 独立设备会话审计索引实体。 */
@Getter
@Setter
@NoArgsConstructor
public class DeviceSessionEntity {

    /** 会话记录主键 ID。 */
    private Long id;

    /** 与 Sa-Token Token 会话绑定的业务会话 ID。 */
    private String sessionId;

    /** 认证主体类型。 */
    private String subjectType;

    /** 认证主体 ID。 */
    private Long subjectId;

    /** 租户 ID，平台身份为空。 */
    private Long tenantId;

    /** 机构 ID，平台身份为空。 */
    private Long organizationId;

    /** 前端应用编码。 */
    private String appCode;

    /** Token 的不可逆摘要。 */
    private String tokenDigest;

    /** 设备稳定标识的不可逆摘要。 */
    private String deviceIdHash;

    /** 设备类型编码。 */
    private String deviceType;

    /** 用户可识别的设备名称。 */
    private String deviceName;

    /** 建立会话所用登录方式。 */
    private String loginMethod;

    /** 由网关确认的完整登录客户端 IP。 */
    private String loginIpAddress;

    /** 登录时间。 */
    private OffsetDateTime loginTime;

    /** 最近活动时间。 */
    private OffsetDateTime lastActiveTime;

    /** 空闲失效时间。 */
    private OffsetDateTime idleExpiresAt;

    /** 绝对失效时间。 */
    private OffsetDateTime absoluteExpiresAt;

    /** 设备会话状态。 */
    private String status;

    /** 权限快照版本。 */
    private Long snapshotVersion;

    /** 乐观锁版本。 */
    private Long version;
}
