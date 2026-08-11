package com.canteen.smile.modules.audit.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** `auth_audit_log` 中由通用注解异步追加的认证安全审计实体。 */
@Getter
@Setter
@NoArgsConstructor
public class AuthAsyncAuditEntity {

    /** 全局唯一事件 ID。 */
    private String eventId;

    /** 审计事件契约版本。 */
    private Integer schemaVersion;

    /** 来源服务或业务域编码。 */
    private String sourceCode;

    /** 任意层级中文分类路径 JSON 数组。 */
    private String categoryPathJson;

    /** 操作人所在应用端编码快照。 */
    private String appCodeSnapshot;

    /** 租户 ID。 */
    private Long tenantId;

    /** 认证主体类型。 */
    private String subjectType;

    /** 认证主体 ID。 */
    private Long subjectId;

    /** 操作人身份类型。 */
    private String operatorType;

    /** 操作人身份 ID。 */
    private Long operatorId;

    /** 操作人用户名快照。 */
    private String operatorUsernameSnapshot;

    /** 操作人显示名称快照。 */
    private String operatorDisplayNameSnapshot;

    /** 稳定动作编码。 */
    private String actionCode;

    /** 中文动作名称快照。 */
    private String actionNameSnapshot;

    /** 被操作目标类型。 */
    private String targetType;

    /** 被操作目标 ID。 */
    private String targetId;

    /** 被操作目标名称快照。 */
    private String targetNameSnapshot;

    /** 被操作目标业务编码快照。 */
    private String targetCodeSnapshot;

    /** 可选操作原因。 */
    private String reason;

    /** SUCCESS、FAILURE 或 DENIED。 */
    private String result;

    /** 可选稳定失败原因码。 */
    private String failureReasonCode;

    /** 可选脱敏手机号。 */
    private String maskedMobile;

    /** 请求链路 ID。 */
    private String traceId;

    /** 业务事件实际发生时间。 */
    private OffsetDateTime occurredTime;

    /** 被审计业务方法执行耗时毫秒数。 */
    private Long durationMs;
}
